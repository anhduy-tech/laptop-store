/* Lapstore – JS nhỏ, không phụ thuộc thư viện */
(function () {
  const vnd = (n) => new Intl.NumberFormat('vi-VN').format(n) + ' ₫';

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

  // 5) Form có data-confirm -> hỏi lại trước khi gửi
  document.querySelectorAll('form[data-confirm]').forEach((f) =>
    f.addEventListener('submit', (e) => { if (!confirm(f.dataset.confirm)) e.preventDefault(); }));
})();
