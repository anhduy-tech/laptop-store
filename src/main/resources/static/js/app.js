/* Lapstore – JS nhỏ, không phụ thuộc thư viện */
(function () {
  const vnd = (n) => new Intl.NumberFormat('vi-VN').format(n) + ' ₫';
  const actionIcons = {
    add: 'M12 5v14M5 12h14',
    edit: 'M12 20h9M16.5 3.5a2.12 2.12 0 013 3L8 18l-4 1 1-4 11.5-11.5z',
    delete: 'M3 6h18M8 6V4h8v2m-11 0 1 14h12l1-14M10 10v6m4-6v6',
    open: 'M14 3h7v7m0-7L10 14M19 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h6',
  };
  const normalized = (text) => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().replace(/\s+/g, ' ').trim();

  function toast(message, kind, timeout) {
    let host = document.getElementById('toasts');
    if (!host) {
      host = document.createElement('div');
      host.id = 'toasts';
      host.className = 'toasts';
      host.setAttribute('aria-live', 'polite');
      document.body.appendChild(host);
    }
    const item = document.createElement('div');
    item.className = 'toast toast-' + (kind || 'success');
    item.setAttribute('role', kind === 'error' ? 'alert' : 'status');
    const text = document.createElement('span');
    text.className = 'toast-message';
    text.textContent = message;
    const close = document.createElement('button');
    close.className = 'toast-close';
    close.type = 'button';
    close.setAttribute('aria-label', 'Đóng thông báo');
    close.textContent = '×';
    close.addEventListener('click', () => dismiss());
    item.append(text, close);
    host.appendChild(item);
    const dismiss = () => {
      if (!item.isConnected) return;
      item.classList.add('toast-leave');
      window.setTimeout(() => item.remove(), 180);
    };
    window.setTimeout(dismiss, timeout || (kind === 'error' ? 8000 : 5000));
    return dismiss;
  }

  window.LapToast = toast;

  function confirmAction(message, title) {
    let dialog = document.getElementById('confirmDialog');
    if (!dialog) {
      dialog = document.createElement('dialog');
      dialog.id = 'confirmDialog';
      dialog.className = 'confirm-dialog';
      dialog.innerHTML = '<form method="dialog" class="confirm-dialog-body"><span class="confirm-icon">!</span><h2 id="confirmTitle"></h2><p id="confirmMessage"></p><div class="confirm-actions"><button value="cancel" class="btn-secondary">Hủy</button><button value="confirm" class="btn-primary">Tiếp tục</button></div></form>';
      document.body.appendChild(dialog);
    }
    const heading = dialog.querySelector('#confirmTitle');
    const copy = dialog.querySelector('#confirmMessage');
    if (heading) heading.textContent = title || 'Xác nhận thao tác';
    if (copy) copy.textContent = message;
    return new Promise((resolve) => {
      dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
      dialog.showModal();
      const cancel = dialog.querySelector('[value="cancel"]');
      if (cancel) cancel.focus();
    });
  }

  window.LapConfirm = confirmAction;

  function initializeTheme() {
    const root = document.documentElement;
    const updateControls = (dark) => {
      document.querySelectorAll('[data-act="toggleTheme"]').forEach((button) => {
        const label = dark ? 'Giao diện sáng' : 'Giao diện tối';
        button.title = label;
        button.setAttribute('aria-label', label);
        const span = button.querySelector('.theme-label');
        if (span) span.textContent = label;
        const path = button.querySelector('svg path');
        if (path) path.setAttribute('d', dark
          ? 'M12 3v2m0 14v2m9-9h-2M5 12H3m15.36 6.36-1.42-1.42M7.06 7.06 5.64 5.64m12.72 0-1.42 1.42M7.06 16.94l-1.42 1.42M16 12a4 4 0 11-8 0 4 4 0 018 0z'
          : 'M20.9 13A9 9 0 1111 3.1 7 7 0 0020.9 13z');
      });
    };
    document.addEventListener('click', (event) => {
      const toggle = event.target.closest('[data-act="toggleTheme"]');
      if (!toggle) return;
      const dark = root.dataset.theme !== 'dark';
      root.dataset.theme = dark ? 'dark' : 'light';
      document.cookie = 'lap_theme=' + (dark ? 'dark' : 'light') + '; Path=/; Max-Age=31536000; SameSite=Lax';
      try { localStorage.setItem('lap_theme', dark ? 'dark' : 'light'); } catch (e) { /* storage may be disabled */ }
      updateControls(dark);
    });
    updateControls(root.dataset.theme === 'dark');
  }

  function initializeFlashToasts() {
    document.querySelectorAll('.alert[role="status"], .alert[role="alert"]').forEach((alert) => {
      const message = alert.textContent.trim();
      if (!message) return;
      const kind = alert.classList.contains('alert-error') || alert.getAttribute('role') === 'alert' ? 'error' : 'success';
      if (kind === 'error') document.documentElement.dataset.flashError = '1';
      alert.remove();
      toast(message, kind);
    });
  }

  function initializeActionIcons() {
    const nodes = document.querySelectorAll('button, a');
    nodes.forEach((node) => {
      if (node.closest('.nav-link, .shop-nav, .pager, .tab-strip')) return;
      const label = node.textContent.trim().replace(/\s+/g, ' ');
      const key = normalized(label).replace(/^[+＋]\s*/, '');
      let action;
      if (/^(don moi|mo don|mo$)/.test(key)) action = 'open';
      else if (/^(sua|chinh sua|cap nhat)/.test(key)) action = 'edit';
      else if (/^xoa(?! loc| bo loc| bo loc)/.test(key)) action = 'delete';
      else if (/^them/.test(key)) action = 'add';
      if (!action || !node.classList.contains('btn-primary') && !node.classList.contains('btn-secondary')
          && !node.classList.contains('btn-danger') && !node.classList.contains('btn-ghost')) return;

      let icon = node.querySelector('svg');
      if (!icon) {
        icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        icon.setAttribute('viewBox', '0 0 24 24');
        icon.setAttribute('fill', 'none');
        icon.setAttribute('stroke', 'currentColor');
        icon.setAttribute('stroke-width', '2');
        icon.setAttribute('stroke-linecap', 'round');
        icon.setAttribute('stroke-linejoin', 'round');
        icon.setAttribute('aria-hidden', 'true');
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', actionIcons[action]);
        icon.appendChild(path);
      }
      icon.classList.add('action-icon-svg');
      node.replaceChildren(icon);
      node.classList.add('btn-icon-reveal');
      node.dataset.actionLabel = label;
      node.setAttribute('aria-label', label);
      if (!node.title) node.title = label;
    });
  }

  function initializeSmartFilters() {
    document.querySelectorAll('.table-wrap .tbl').forEach((table) => {
      const tableWrap = table.closest('.table-wrap');
      if (!tableWrap || tableWrap.dataset.smartFilter === 'ready') return;
      const headers = Array.from(table.querySelectorAll('thead th')).map((th) => th.textContent.trim());
      const rows = Array.from(table.querySelectorAll('tbody tr')).filter((row) => row.querySelectorAll('td').length);
      if (headers.length < 2 || rows.length < 1) return;
      tableWrap.dataset.smartFilter = 'ready';

      const outer = document.createElement('div');
      outer.className = 'smart-table-wrap';
      tableWrap.parentNode.insertBefore(outer, tableWrap);
      const toolbar = document.createElement('div');
      toolbar.className = 'smart-filter';
      const search = document.createElement('input');
      search.type = 'search';
      search.className = 'input-field smart-filter-search';
      search.placeholder = 'Tìm nhanh trên mọi cột...';
      search.setAttribute('aria-label', 'Tìm nhanh trên mọi cột');
      toolbar.appendChild(search);
      const clear = document.createElement('button');
      clear.type = 'button';
      clear.className = 'btn-ghost btn-sm';
      clear.textContent = 'Xóa lọc';
      clear.addEventListener('click', () => {
        search.value = '';
        controls.forEach((control) => { control.value = ''; });
        apply();
      });
      toolbar.appendChild(clear);

      const advanced = document.createElement('details');
      advanced.className = 'smart-filter-advanced';
      const summary = document.createElement('summary');
      summary.className = 'btn-secondary btn-sm';
      summary.textContent = 'Lọc nâng cao';
      const fields = document.createElement('div');
      fields.className = 'smart-filter-fields';
      const controls = [];
      headers.forEach((header, index) => {
        if (!header || index >= 8) return;
        const values = Array.from(new Set(rows.map((row) => row.cells[index]?.textContent.trim() || '').filter(Boolean)));
        if (!values.length) return;
        const label = document.createElement('label');
        label.textContent = header;
        let control;
        if (values.length <= 12) {
          control = document.createElement('select');
          control.innerHTML = '<option value="">Tất cả</option>';
          values.sort((a, b) => a.localeCompare(b, 'vi')).forEach((value) => {
            const option = document.createElement('option');
            option.value = normalized(value);
            option.textContent = value;
            control.appendChild(option);
          });
        } else {
          control = document.createElement('input');
          control.type = 'search';
          control.placeholder = 'Nhập ' + header.toLowerCase();
        }
        control.className = 'input-field';
        control.dataset.column = String(index);
        control.setAttribute('aria-label', 'Lọc theo ' + header);
        control.addEventListener('input', apply);
        control.addEventListener('change', apply);
        label.appendChild(control);
        fields.appendChild(label);
        controls.push(control);
      });
      advanced.append(summary, fields);
      toolbar.appendChild(advanced);
      const count = document.createElement('span');
      count.className = 'smart-filter-count';
      toolbar.appendChild(count);
      outer.append(toolbar, tableWrap);

      const empty = document.createElement('div');
      empty.className = 'smart-filter-empty';
      empty.hidden = true;
      empty.textContent = 'Không có dòng nào khớp bộ lọc.';
      outer.appendChild(empty);

      function apply() {
        const needle = normalized(search.value);
        let visible = 0;
        rows.forEach((row) => {
          const matchesSearch = !needle || normalized(row.textContent).includes(needle);
          const matchesFields = controls.every((control) => {
            const cell = normalized(row.cells[Number(control.dataset.column)]?.textContent || '');
            const value = normalized(control.value);
            return !value || (control.tagName === 'SELECT' ? cell === value : cell.includes(value));
          });
          row.hidden = !(matchesSearch && matchesFields);
          if (!row.hidden) visible++;
        });
        count.textContent = visible + ' / ' + rows.length + ' dòng trên trang hiện tại';
        empty.hidden = visible !== 0;
      }

      search.addEventListener('input', apply);
      apply();
    });
  }

  // 1) Sidebar: link sang portal khác (khác cổng) -> ghép host hiện tại + data-port
  document.querySelectorAll('a[data-port]').forEach((a) => {
    a.href = location.protocol + '//' + location.hostname + ':' + a.dataset.port + a.getAttribute('data-path');
    if (window.self !== window.top) a.target = '_top'; // đang trong tab (iframe): sang phân hệ khác thì đổi cả cửa sổ
  });

  // 1b) Liên kết có data-newtab: trong khung tab thì mở ở một tab mới của workspace (Ctrl-click vẫn mở cửa sổ mới)
  document.addEventListener('click', (e) => {
    const a = e.target.closest && e.target.closest('a[data-newtab]');
    if (!a || e.ctrlKey || e.metaKey || e.shiftKey || e.button !== 0) return;
    if (window.parent !== window && window.parent.Workspace) {
      e.preventDefault();
      window.parent.Workspace.open(a.href, { title: a.dataset.title || a.textContent.trim() });
    }
  });

  // 2) SH-02: đổi variant -> cập nhật giá
  const sel = document.getElementById('variantId');
  const price = document.getElementById('variantPrice');
  if (sel && price) {
    const upd = () => { price.textContent = vnd(sel.selectedOptions[0].dataset.price); };
    sel.addEventListener('change', upd); upd();
  }

  // 3) SH-04: ẩn/hiện địa chỉ khi chọn hình thức nhận
  const box = document.getElementById('addressBox');
  if (box) {
    const radios = document.querySelectorAll('input[name=receiveMethod]');
    const upd = () => {
      const delivery = [...radios].some((r) => r.checked && r.value === 'DELIVERY');
      box.hidden = !delivery;
      box.querySelectorAll('textarea,input').forEach((i) => (i.required = delivery));
    };
    radios.forEach((r) => r.addEventListener('change', upd)); upd();
  }

  // 4) CO-02: cuộn chat xuống cuối
  const log = document.getElementById('chatLog');
  if (log) log.scrollTop = log.scrollHeight;

  // 5) Confirm an action with the shared custom dialog.
  const confirmedForms = new WeakSet();
  document.addEventListener('submit', (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || !form.dataset.confirm) return;
    if (confirmedForms.has(form)) {
      confirmedForms.delete(form);
      return;
    }
    event.preventDefault();
    confirmAction(form.dataset.confirm).then((confirmed) => {
      if (!confirmed) return;
      if (!form.reportValidity()) return;
      confirmedForms.add(form);
      if (form.requestSubmit) form.requestSubmit();
      else form.submit();
    });
  }, true);

  initializeTheme();
  initializeFlashToasts();
  initializeActionIcons();
  initializeSmartFilters();

  const uiObserver = new MutationObserver((records) => {
    if (records.some((record) => Array.from(record.addedNodes).some((node) => node.nodeType === Node.ELEMENT_NODE))) {
      initializeActionIcons();
      initializeSmartFilters();
    }
  });
  uiObserver.observe(document.body, { childList: true, subtree: true });
})();
