/* Màn bán quầy: đếm ngược giữ máy, ô tìm khách, popup chọn sản phẩm (lọc cấu hình -> chọn serial), trang thanh toán.
 * Không phụ thuộc thư viện. Chỉ GET JSON từ /staff/sales/counter/api; mọi thay đổi dữ liệu đi qua form POST thường (PRG). */
(function () {
  'use strict';

  var root = document.querySelector('.pos');
  if (!root) return;
  var api = root.getAttribute('data-api');
  var nf = new Intl.NumberFormat('vi-VN');

  function $(sel, ctx) { return (ctx || document).querySelector(sel); }
  function $$(sel, ctx) { return Array.prototype.slice.call((ctx || document).querySelectorAll(sel)); }
  function money(n) { return nf.format(Math.round(n || 0)) + ' ₫'; }
  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }
  function debounce(fn, ms) {
    var t;
    return function () { var a = arguments, c = this; clearTimeout(t); t = setTimeout(function () { fn.apply(c, a); }, ms); };
  }
  function getJson(url) {
    return fetch(url, { headers: { Accept: 'application/json' }, credentials: 'same-origin' }).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }
  function fmtLeft(s) {
    if (s <= 0) return 'Hết hạn';
    var m = Math.floor(s / 60), x = s % 60;
    return m + ':' + (x < 10 ? '0' : '') + x;
  }

  // ------------------------------------------------------------------ 1) đếm ngược giữ máy
  var timers = $$('.pos-left[data-left]');
  if (timers.length) {
    var left = timers.map(function (e) { return parseInt(e.getAttribute('data-left'), 10) || 0; });
    var paint = function () {
      timers.forEach(function (e, i) {
        e.textContent = fmtLeft(left[i]);
        e.classList.toggle('is-low', left[i] > 0 && left[i] <= 120);
        e.classList.toggle('is-out', left[i] <= 0);
      });
    };
    paint();
    setInterval(function () { for (var i = 0; i < left.length; i++) if (left[i] > 0) left[i]--; paint(); }, 1000);
  }

  // ------------------------------------------------------------------ 2) tìm khách
  var custQ = $('#custQ');
  if (custQ) {
    var list = $('#custList'), hot = -1, hits = [];
    var close = function () { list.hidden = true; hot = -1; };
    var pick = function (c) { $('#custId').value = c.id; $('#custPick').submit(); };
    var render = function () {
      list.innerHTML = '';
      if (!hits.length) {
        list.appendChild(el('li', 'is-none', 'Không thấy khách — dùng “Thêm nhanh khách mới” bên dưới'));
      }
      hits.forEach(function (c, i) {
        var li = el('li', i === hot ? 'is-hot' : '');
        li.appendChild(el('span', 'font-medium', c.name));
        li.appendChild(el('span', 'pos-muted', c.phone));
        li.addEventListener('mousedown', function (ev) { ev.preventDefault(); pick(c); });
        list.appendChild(li);
      });
      list.hidden = false;
    };
    var search = debounce(function () {
      var q = custQ.value.trim();
      if (q.length < 2) { close(); return; }
      getJson(api + '/customers?q=' + encodeURIComponent(q)).then(function (r) { hits = r; hot = -1; render(); })
        .catch(function () { close(); });
    }, 250);
    custQ.addEventListener('input', search);
    custQ.addEventListener('blur', function () { setTimeout(close, 120); });
    custQ.addEventListener('keydown', function (e) {
      if (list.hidden || !hits.length) return;
      if (e.key === 'ArrowDown') { hot = Math.min(hits.length - 1, hot + 1); render(); e.preventDefault(); }
      else if (e.key === 'ArrowUp') { hot = Math.max(0, hot - 1); render(); e.preventDefault(); }
      else if (e.key === 'Enter' && hot >= 0) { pick(hits[hot]); e.preventDefault(); }
      else if (e.key === 'Escape') { close(); }
    });
  }

  // ------------------------------------------------------------------ 3) popup chọn sản phẩm
  var dlg = $('#productDialog');
  if (dlg) {
    var optionsLoaded = false, current = null, selected = {};
    var f = {
      q: $('#fQ'), brand: $('#fBrand'), cpu: $('#fCpu'), ram: $('#fRam'), storage: $('#fStorage'), gpu: $('#fGpu'),
      min: $('#fMin'), max: $('#fMax'), avail: $('#fAvail')
    };
    var body = $('#resBody'), count = $('#resCount'), pane = $('#serialPane');

    var fill = function (sel, values, label) {
      values.forEach(function (v) {
        var o = document.createElement('option');
        o.value = v.id != null ? v.id : v;
        o.textContent = v.name != null ? v.name : (label ? label(v) : v);
        sel.appendChild(o);
      });
    };
    var loadOptions = function () {
      if (optionsLoaded) return;
      optionsLoaded = true;
      getJson(api + '/options').then(function (o) {
        fill(f.brand, o.brands);
        fill(f.cpu, o.cpus);
        fill(f.gpu, o.gpus);
        fill(f.ram, o.rams, function (v) { return v + ' GB'; });
        fill(f.storage, o.storages, function (v) { return v >= 1024 && v % 1024 === 0 ? (v / 1024) + ' TB' : v + ' GB'; });
      }).catch(function () { optionsLoaded = false; });
    };

    var query = function () {
      var p = [];
      var add = function (k, v) { if (v !== '' && v != null) p.push(k + '=' + encodeURIComponent(v)); };
      add('q', f.q.value.trim());
      add('brandId', f.brand.value); add('cpu', f.cpu.value); add('gpu', f.gpu.value);
      add('ram', f.ram.value); add('storage', f.storage.value);
      add('minPrice', f.min.value); add('maxPrice', f.max.value);
      p.push('onlyAvailable=' + (f.avail.checked ? 'true' : 'false'));
      return p.join('&');
    };

    var spec = function (r) {
      var parts = [];
      if (r.cpu) parts.push(r.cpu);
      if (r.ramGb) parts.push(r.ramGb + 'GB RAM');
      if (r.storageGb) parts.push(r.storageGb >= 1024 && r.storageGb % 1024 === 0 ? (r.storageGb / 1024) + 'TB' : r.storageGb + 'GB');
      if (r.gpu) parts.push(r.gpu);
      return parts.join(' · ');
    };

    var run = function () {
      count.textContent = 'Đang tìm…';
      getJson(api + '/products?' + query()).then(function (rows) {
        body.innerHTML = '';
        count.textContent = rows.length ? rows.length + ' kết quả' + (rows.length >= 60 ? ' (hiển thị 60 đầu — hãy lọc thêm)' : '') : 'Không có sản phẩm phù hợp.';
        rows.forEach(function (r) {
          var tr = document.createElement('tr');
          var c1 = el('td'); c1.appendChild(el('div', 'font-medium', r.productName));
          c1.appendChild(el('div', 'pos-muted', (r.color ? r.color + ' · ' : '') + r.sku)); tr.appendChild(c1);
          tr.appendChild(el('td', 'pos-spec', spec(r)));
          tr.appendChild(el('td', '', r.branchName));
          tr.appendChild(el('td', 'text-right font-semibold', money(r.price)));
          var c5 = el('td', 'text-center'); c5.appendChild(el('span', 'pos-avail' + (r.available <= 0 ? ' is-zero' : ''), String(r.available)));
          tr.appendChild(c5);
          var c6 = el('td', 'text-right'); var b = el('button', 'btn-secondary btn-sm', 'Chọn serial'); b.type = 'button';
          b.disabled = r.available <= 0;
          b.addEventListener('click', function () {
            $$('tr.is-sel', body).forEach(function (x) { x.classList.remove('is-sel'); });
            tr.classList.add('is-sel');
            showSerials(r);
          });
          c6.appendChild(b); tr.appendChild(c6);
          body.appendChild(tr);
        });
      }).catch(function () { count.textContent = 'Không tải được danh sách, thử lại.'; });
    };
    var runSoon = debounce(run, 300);

    var syncSerials = function () {
      var keys = Object.keys(selected);
      $('#serialCount').textContent = keys.length + ' đã chọn';
      $('#holdManySerials').value = keys.join('\n');
      $('#addSerials').disabled = keys.length === 0;
      $('#addSerials').textContent = keys.length ? 'Thêm ' + keys.length + ' máy vào đơn' : 'Thêm vào đơn';
      $('#serialAll').checked = keys.length > 0 && keys.length === $$('input[type=checkbox]', $('#serialList')).length;
    };
    var showSerials = function (r) {
      current = r; selected = {};
      pane.hidden = false;
      $('#serialTitle').textContent = r.productName;
      $('#serialSub').textContent = (r.variantName || '') + ' · ' + r.branchName + ' · ' + money(r.price);
      var ul = $('#serialList'); ul.innerHTML = '';
      ul.appendChild(el('li', 'pos-muted', 'Đang tải…'));
      syncSerials();
      getJson(api + '/serials?variantId=' + r.variantId + '&branchId=' + r.branchId).then(function (rows) {
        ul.innerHTML = '';
        if (!rows.length) { ul.appendChild(el('li', 'pos-muted', 'Không còn máy nào bán được.')); return; }
        rows.forEach(function (s) {
          var li = document.createElement('li'), lb = el('label'), cb = document.createElement('input');
          cb.type = 'checkbox'; cb.value = s.serial;
          cb.addEventListener('change', function () { if (cb.checked) selected[s.serial] = 1; else delete selected[s.serial]; syncSerials(); });
          lb.appendChild(cb); lb.appendChild(el('span', '', s.serial));
          li.appendChild(lb); ul.appendChild(li);
        });
      }).catch(function () { ul.innerHTML = ''; ul.appendChild(el('li', 'pos-muted', 'Không tải được serial.')); });
    };
    $('#serialAll').addEventListener('change', function (e) {
      selected = {};
      $$('input[type=checkbox]', $('#serialList')).forEach(function (cb) {
        cb.checked = e.target.checked;
        if (cb.checked) selected[cb.value] = 1;
      });
      syncSerials();
    });

    $('#openProducts').addEventListener('click', function () {
      if (!dlg.open) dlg.showModal();
      loadOptions(); run(); f.q.focus();
    });
    $('#closeProducts').addEventListener('click', function () { dlg.close(); });
    dlg.addEventListener('click', function (e) { if (e.target === dlg) dlg.close(); });
    f.q.addEventListener('input', runSoon);
    f.min.addEventListener('input', runSoon);
    f.max.addEventListener('input', runSoon);
    [f.brand, f.cpu, f.ram, f.storage, f.gpu, f.avail].forEach(function (s) { s.addEventListener('change', run); });
    $('#fClear').addEventListener('click', function () {
      f.q.value = ''; f.min.value = ''; f.max.value = '';
      [f.brand, f.cpu, f.ram, f.storage, f.gpu].forEach(function (s) { s.value = ''; });
      f.avail.checked = true; run();
    });
    $('#holdManyForm').addEventListener('submit', function (e) { if (!Object.keys(selected).length) e.preventDefault(); });
  }

  // ------------------------------------------------------------------ 4) trang thanh toán
  var form = $('#payForm');
  if (form) {
    var total = parseInt(root.getAttribute('data-total'), 10) || 0;
    var tendered = $('#tendered'), cash = $('#cashBox'), ref = $('#refBox'), btn = $('#payBtn');
    var changeBox = $('#changeVal').parentNode;
    var method = function () { return ($('input[name=method]:checked', form) || {}).value || 'CASH'; };

    var calc = function () {
      var isCash = method() === 'CASH';
      cash.hidden = !isCash; ref.hidden = isCash;
      tendered.disabled = !isCash;
      if (!isCash) { btn.disabled = false; return; }
      var t = parseInt(tendered.value, 10);
      if (isNaN(t)) t = 0;
      var diff = t - total;
      changeBox.classList.toggle('is-short', diff < 0);
      $('#changeLabel').textContent = diff < 0 ? 'Còn thiếu' : 'Tiền thối';
      $('#changeVal').textContent = money(Math.abs(diff));
      btn.disabled = diff < 0;
    };

    // gợi ý mệnh giá: đúng số tiền, rồi làm tròn lên các mốc thường gặp
    var quick = $('#quickCash'), seen = {};
    var addQuick = function (v, label) {
      if (v < total || seen[v]) return; seen[v] = 1;
      var b = el('button', '', label || money(v)); b.type = 'button';
      b.addEventListener('click', function () { tendered.value = v; calc(); });
      quick.appendChild(b);
    };
    addQuick(total, 'Đủ ' + money(total));
    [100000, 500000, 1000000, 5000000, 10000000].forEach(function (step) {
      var v = Math.ceil(total / step) * step; if (v > total) addQuick(v);
    });

    $$('input[name=method]', form).forEach(function (r) { r.addEventListener('change', calc); });
    tendered.addEventListener('input', calc);
    form.addEventListener('submit', function (e) {
      if (btn.disabled) { e.preventDefault(); return; }
      btn.disabled = true; btn.textContent = 'Đang xử lý…'; // tránh bấm hai lần
    });
    calc();
    tendered.focus(); tendered.select();
  }
})();
