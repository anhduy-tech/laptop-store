/* Khung làm việc nhân viên: bố cục sidebar | top menu, thu gọn sidebar, và hệ thống TAB.
 *
 * Tab: mỗi trang mở trong một iframe riêng (cùng origin) và được giữ sống khi chuyển tab, nên trạng thái đang nhập dở
 * (VD các ô đã tick ở Nhập kho) không mất khi sang Chuyển kho rồi quay lại. Không có JS thì các liên kết vẫn chạy như
 * trang thường. Trang chạy trong iframe được layout đánh dấu `html.embedded` (ẩn khung điều hướng).
 *
 * Phần logic thuần (WorkspaceLogic) không đụng DOM nên chạy thử được bằng Node. */
(function (root) {
  'use strict';

  var MAX_TABS = 10;
  var KEY_LAYOUT = 'lap_layout';
  var KEY_COLLAPSED = 'lap_collapsed';
  var KEY_TABS = 'lap_tabs';

  // ------------------------------------------------------------------ logic thuần

  /** Đường dẫn + query (bỏ origin và #hash) của một URL cùng origin; khác origin trả nguyên chuỗi. */
  function fullOf(url, base) {
    try {
      var b = new URL(base);
      var u = new URL(url, b);
      return u.origin === b.origin ? u.pathname + u.search : String(url);
    } catch (e) { return String(url); }
  }

  /** Chỉ đường dẫn: danh tính của một tab (đổi query không tạo tab mới). */
  function pathOf(url, base) {
    var f = fullOf(url, base);
    var i = f.indexOf('?');
    return i >= 0 ? f.slice(0, i) : f;
  }

  /** "Nhập kho – Lapstore" -> "Nhập kho". */
  function titleFrom(t) {
    return String(t == null ? '' : t).replace(/\s+[–-]\s+Lapstore.*$/i, '').trim();
  }

  /**
   * Mở (hoặc kích hoạt) tab theo đường dẫn. Trả {tabs, activeId, status} với status: 'created' | 'existing' | 'limit'.
   * @param nextId function() -> id mới
   */
  function openTab(tabs, url, title, base, max, nextId, navigate) {
    var f = fullOf(url, base);
    var p = pathOf(url, base);
    for (var i = 0; i < tabs.length; i++) {
      if (pathOf(tabs[i].url, base) === p) {
        if (navigate && tabs[i].url !== f) tabs[i].url = f;
        return { tabs: tabs, activeId: tabs[i].id, status: 'existing', navigated: !!navigate && true };
      }
    }
    if (tabs.length >= max) return { tabs: tabs, activeId: null, status: 'limit' };
    var t = { id: nextId(), url: f, title: title || 'Đang tải…' };
    tabs.push(t);
    return { tabs: tabs, activeId: t.id, status: 'created' };
  }

  /** Đóng một tab; nếu đóng tab đang xem thì chuyển sang tab bên trái (hoặc bên phải nếu là tab đầu). */
  function closeTab(tabs, activeId, closeId) {
    var idx = -1;
    for (var i = 0; i < tabs.length; i++) if (tabs[i].id === closeId) idx = i;
    if (idx < 0) return { tabs: tabs, activeId: activeId };
    tabs.splice(idx, 1);
    if (activeId !== closeId) return { tabs: tabs, activeId: activeId };
    if (!tabs.length) return { tabs: tabs, activeId: null };
    return { tabs: tabs, activeId: tabs[idx > 0 ? idx - 1 : 0].id };
  }

  /**
   * Tab khác đang mở cùng đường dẫn với `tab` (sau khi tab này điều hướng). Có thì hai tab phải gộp: VD đang ở tab "Chi tiết sản phẩm"
   * bấm "← Danh sách" mà tab "Sản phẩm" đã mở sẵn, thì đóng tab chi tiết và quay về tab danh sách.
   */
  function findDuplicateTab(tabs, tab, base) {
    var p = pathOf(tab.url, base);
    for (var i = 0; i < tabs.length; i++) {
      if (tabs[i].id !== tab.id && pathOf(tabs[i].url, base) === p) return tabs[i];
    }
    return null;
  }

  root.WorkspaceLogic = { fullOf: fullOf, pathOf: pathOf, titleFrom: titleFrom, openTab: openTab, closeTab: closeTab,
    findDuplicateTab: findDuplicateTab, MAX_TABS: MAX_TABS };

  if (typeof document === 'undefined') return;

  // ------------------------------------------------------------------ giao diện

  var html = document.documentElement;
  if (window.self !== window.top || html.classList.contains('embedded')) return; // trang trong tab: không làm gì

  var $ = function (sel, ctx) { return (ctx || document).querySelector(sel); };
  var $$ = function (sel, ctx) { return Array.prototype.slice.call((ctx || document).querySelectorAll(sel)); };
  var desktop = window.matchMedia('(min-width: 1024px)');

  function store(key, val) { try { localStorage.setItem(key, val); } catch (e) { /* bỏ qua */ } }

  // ---- bố cục
  function setLayout(mode) {
    html.dataset.layout = mode;
    store(KEY_LAYOUT, mode);
    $$('[data-layout-choice]').forEach(function (b) {
      b.setAttribute('aria-pressed', String(b.dataset.layoutChoice === mode));
    });
  }

  function toggleCollapsed() {
    if (html.dataset.layout !== 'sidebar') return;
    var on = html.dataset.collapsed !== '1';
    html.dataset.collapsed = on ? '1' : '0';
    store(KEY_COLLAPSED, on ? '1' : '0');
  }

  function setDrawer(open) {
    if (open) html.dataset.drawer = 'open'; else delete html.dataset.drawer;
  }

  function toast(msg, kind) {
    var box = $('#toasts');
    if (!box) return;
    var t = document.createElement('div');
    t.className = 'toast toast-' + (kind || 'success');
    t.setAttribute('role', kind === 'error' ? 'alert' : 'status');
    var span = document.createElement('span');
    span.textContent = msg;
    t.appendChild(span);
    box.appendChild(t);
    setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, kind === 'error' ? 9000 : 6000);
  }

  // ---- tab
  var enabled = html.dataset.workspace !== 'off';
  var strip = $('#tabStrip');
  var host = $('#tabHost');
  var own = $('#ownContent');
  var home = document.body.dataset.home || '/';
  var state = { tabs: [], active: null };
  var frames = {};
  var seq = 0;
  var swapped = false;

  function nextId() { seq += 1; return 't' + Date.now().toString(36) + seq; }
  function full(url) { return fullOf(url, location.href); }
  function pathOfUrl(url) { return pathOf(url, location.href); }
  function byId(id) { return state.tabs.filter(function (t) { return t.id === id; })[0]; }

  function persist() {
    try {
      sessionStorage.setItem(KEY_TABS, JSON.stringify({ tabs: state.tabs, active: state.active }));
    } catch (e) { /* bỏ qua */ }
  }

  function isDirty(id) {
    try { return frames[id].contentDocument.documentElement.dataset.dirty === '1'; } catch (e) { return false; }
  }

  function navLabelFor(url) {
    var p = pathOfUrl(url);
    var hit = $$('a.nav-link[data-tab]').filter(function (a) { return pathOfUrl(a.getAttribute('href')) === p; })[0];
    return hit ? hit.textContent.trim() : '';
  }

  function renderStrip() {
    strip.innerHTML = '';
    state.tabs.forEach(function (t) {
      var el = document.createElement('div');
      el.className = 'tab' + (t.id === state.active ? ' tab-on' : '');
      el.setAttribute('role', 'tab');
      el.setAttribute('aria-selected', String(t.id === state.active));
      el.dataset.id = t.id;
      el.title = t.title;
      var title = document.createElement('span');
      title.className = 'tab-title';
      title.textContent = t.title;
      var dot = document.createElement('span');
      dot.className = 'tab-dirty';
      dot.title = 'Có dữ liệu chưa lưu';
      dot.hidden = !isDirty(t.id);
      var close = document.createElement('button');
      close.type = 'button';
      close.className = 'tab-close';
      close.setAttribute('aria-label', 'Đóng tab ' + t.title);
      close.textContent = '×';
      close.dataset.close = t.id;
      el.appendChild(title);
      el.appendChild(dot);
      el.appendChild(close);
      strip.appendChild(el);
    });
    var count = $('#tabCount');
    if (count) count.textContent = state.tabs.length;
  }

  function refreshDirtyDots() {
    $$('.tab', strip).forEach(function (el) {
      var dot = $('.tab-dirty', el);
      if (dot) dot.hidden = !isDirty(el.dataset.id);
    });
  }

  function highlightNav() {
    var t = byId(state.active);
    if (!t) return;
    var cur = pathOfUrl(t.url);
    var best = null;
    $$('a.nav-link[data-tab]').forEach(function (a) {
      var p = pathOfUrl(a.getAttribute('href'));
      var hit = a.dataset.prefix ? cur.indexOf(a.dataset.prefix) === 0
        : (a.dataset.exact === 'true' ? (cur === p || cur === p + '/') : (cur === p || cur.indexOf(p + '/') === 0));
      if (hit && (!best || p.length > best.p.length)) best = { a: a, p: p };
    });
    $$('a.nav-link[data-tab]').forEach(function (a) { a.classList.toggle('nav-active', !!best && best.a === a); });
  }

  function syncAddress() {
    var t = byId(state.active);
    if (t && full(location.href) !== t.url) {
      try { history.replaceState(null, '', t.url); } catch (e) { /* bỏ qua */ }
    }
  }

  function swapToHost() {
    if (swapped) return;
    swapped = true;
    if (own) own.hidden = true;
    host.hidden = false;
  }

  /** Đóng tab `gone` (vừa điều hướng tới trang mà tab `keep` đang mở) và quay về `keep`, làm mới nếu không có dữ liệu nhập dở. */
  function mergeInto(keep, gone) {
    var wasActive = state.active === gone.id;
    var gf = frames[gone.id];
    if (gf && gf.parentNode) gf.parentNode.removeChild(gf);
    delete frames[gone.id];
    state.tabs = state.tabs.filter(function (t) { return t.id !== gone.id; });
    var kf = frames[keep.id];
    if (!kf || !isDirty(keep.id)) {
      keep.url = gone.url;
      if (kf) kf.src = keep.url; // nạp lại để thấy dữ liệu mới (VD vừa sửa sản phẩm xong)
    }
    if (wasActive) { activate(keep.id); } else { renderStrip(); persist(); }
  }

  function onFrameLoad(tab, frame) {
    try {
      var w = frame.contentWindow;
      tab.url = full(w.location.href);
      var t = titleFrom(w.document.title);
      if (t) tab.title = t;
      frame.title = tab.title;
    } catch (e) { /* khác origin: giữ nguyên */ }
    var dup = findDuplicateTab(state.tabs, tab, location.href);
    if (dup) { mergeInto(dup, tab); return; }
    if (tab.id === state.active) swapToHost();
    renderStrip();
    persist();
    syncAddress();
    highlightNav();
  }

  function ensureFrame(tab) {
    if (frames[tab.id]) return frames[tab.id];
    var f = document.createElement('iframe');
    f.title = tab.title;
    f.hidden = true;
    f.addEventListener('load', function () { onFrameLoad(tab, f); });
    f.src = tab.url;
    host.appendChild(f);
    frames[tab.id] = f;
    return f;
  }

  function activate(id) {
    var tab = byId(id);
    if (!tab) return;
    state.active = id;
    ensureFrame(tab);
    Object.keys(frames).forEach(function (k) { frames[k].hidden = k !== id; });
    if (swapped) { own.hidden = true; host.hidden = false; }
    renderStrip();
    persist();
    syncAddress();
    highlightNav();
  }

  function openTabUrl(url, opts) {
    opts = opts || {};
    if (!enabled) { location.href = url; return; }
    var res = openTab(state.tabs, url, opts.title || navLabelFor(url), location.href, MAX_TABS, nextId, !!opts.navigate);
    if (res.status === 'limit') { toast('Đang mở tối đa ' + MAX_TABS + ' tab. Hãy đóng bớt trước khi mở thêm.', 'error'); return; }
    var tab = byId(res.activeId);
    if (res.status === 'existing' && opts.navigate && frames[tab.id]) frames[tab.id].src = tab.url;
    activate(res.activeId);
  }

  function closeTabById(id) {
    if (isDirty(id) && !confirm('Tab này có dữ liệu chưa lưu. Đóng và bỏ dữ liệu?')) return;
    var f = frames[id];
    if (f && f.parentNode) f.parentNode.removeChild(f);
    delete frames[id];
    var res = closeTab(state.tabs, state.active, id);
    if (!res.activeId) { openTabUrl(home, { navigate: true }); return; }
    activate(res.activeId);
  }

  function closeAll() {
    var dirty = state.tabs.some(function (t) { return isDirty(t.id); });
    if (dirty && !confirm('Có tab chứa dữ liệu chưa lưu. Đóng tất cả và bỏ dữ liệu?')) return;
    Object.keys(frames).forEach(function (k) { if (frames[k].parentNode) frames[k].parentNode.removeChild(frames[k]); });
    frames = {};
    state.tabs = [];
    state.active = null;
    openTabUrl(home, { navigate: true });
  }

  // ---- khởi tạo
  setLayout(html.dataset.layout === 'topmenu' ? 'topmenu' : 'sidebar');

  if (enabled && strip && host && own) {
    var saved = null;
    try { saved = JSON.parse(sessionStorage.getItem(KEY_TABS) || 'null'); } catch (e) { saved = null; }
    if (saved && Array.isArray(saved.tabs)) state.tabs = saved.tabs.filter(function (t) { return t && t.id && t.url; });

    // thông báo (flash) của lần tải trang đầu: chuyển thành toast vì nội dung sẽ nằm trong iframe
    $$('.alert', own).forEach(function (a) {
      toast(a.textContent.trim(), a.classList.contains('alert-error') ? 'error' : 'success');
    });

    var here = full(location.href);
    var cur = state.tabs.filter(function (t) { return pathOfUrl(t.url) === pathOfUrl(here); })[0];
    if (cur) { cur.url = here; } else {
      if (state.tabs.length >= MAX_TABS) state.tabs.shift();
      cur = { id: nextId(), url: here, title: titleFrom(document.title) || navLabelFor(here) || 'Trang' };
      state.tabs.push(cur);
    }
    activate(cur.id);

    strip.addEventListener('click', function (e) {
      var c = e.target.closest('[data-close]');
      if (c) { e.stopPropagation(); closeTabById(c.dataset.close); return; }
      var tabEl = e.target.closest('.tab');
      if (tabEl) activate(tabEl.dataset.id);
    });
    strip.addEventListener('auxclick', function (e) {
      if (e.button !== 1) return;
      var tabEl = e.target.closest('.tab');
      if (tabEl) { e.preventDefault(); closeTabById(tabEl.dataset.id); }
    });
    setInterval(refreshDirtyDots, 2000);

    // menu điều hướng: mở/kích hoạt tab thay vì tải lại cả khung
    $$('a.nav-link[data-tab]').forEach(function (a) {
      a.addEventListener('click', function (e) {
        if (e.ctrlKey || e.metaKey || e.shiftKey || e.button !== 0) return;
        e.preventDefault();
        openTabUrl(a.getAttribute('href'), { title: a.textContent.trim() });
        setDrawer(false);
      });
    });

    var search = $('#quickSearch');
    if (search) {
      var sInput = $('.search-input', search);
      var sBtn = $('.search-toggle', search);
      var setSearch = function (open) {
        search.dataset.open = open ? 'true' : 'false';
        sBtn.setAttribute('aria-expanded', String(open));
        if (open) { sInput.removeAttribute('tabindex'); sInput.focus(); } else { sInput.setAttribute('tabindex', '-1'); }
      };
      sBtn.addEventListener('mousedown', function (e) { e.preventDefault(); }); // giữ tiêu điểm để blur không đóng trước khi click
      sBtn.addEventListener('click', function () {
        if (search.dataset.open !== 'true') { setSearch(true); return; }
        if (sInput.value.trim()) search.requestSubmit(); else setSearch(false);
      });
      sInput.addEventListener('blur', function () {
        setTimeout(function () { if (!sInput.value.trim() && document.activeElement !== sInput) setSearch(false); }, 120);
      });
      sInput.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') { sInput.value = ''; setSearch(false); sInput.blur(); }
      });
      search.addEventListener('submit', function (e) {
        e.preventDefault();
        var q = sInput.value.trim();
        if (!q) { setSearch(false); return; }
        openTabUrl(search.getAttribute('action') + '?q=' + encodeURIComponent(q), { title: 'Sản phẩm', navigate: true });
        setDrawer(false);
      });
    }

    // trang trong iframe gọi lên: parent.Workspace.open(href, {title})
    root.Workspace = { open: openTabUrl, closeAll: closeAll };
  } else {
    // không dùng tab (trang xem thử / thiếu thành phần): dựng lại nút đóng tất cả cho khỏi lỗi
    root.Workspace = { open: function (u) { location.href = u; }, closeAll: function () {} };
  }

  // ---- nút điều khiển bố cục
  $('#navToggle').addEventListener('click', function () {
    if (desktop.matches) toggleCollapsed(); else setDrawer(html.dataset.drawer !== 'open');
  });
  $('#navBackdrop').addEventListener('click', function () { setDrawer(false); });
  desktop.addEventListener('change', function () { setDrawer(false); });

  var dlg = $('#settingsDialog');
  document.addEventListener('click', function (e) {
    var choice = e.target.closest('[data-layout-choice]');
    if (choice) { setLayout(choice.dataset.layoutChoice); return; }
    var act = e.target.closest('[data-act]');
    if (!act) return;
    switch (act.dataset.act) {
      case 'openSettings': renderStrip(); if (dlg && dlg.showModal) dlg.showModal(); setDrawer(false); break;
      case 'closeSettings': if (dlg) dlg.close(); break;
      case 'toggleCollapse': toggleCollapsed(); break;
      case 'closeAllTabs': if (enabled) { closeAll(); if (dlg) dlg.close(); } break;
      default: break;
    }
  });
  if (dlg) dlg.addEventListener('click', function (e) { if (e.target === dlg) dlg.close(); });
})(typeof window !== 'undefined' ? window : globalThis);
