/* Lapstore – JS nhỏ, không phụ thuộc thư viện */
(function () {
  const vnd = (n) => new Intl.NumberFormat('vi-VN').format(n) + ' ₫';

  // 1) Sidebar: link sang portal khác (khác cổng) → ghép host hiện tại + data-port
  document.querySelectorAll('a[data-port]').forEach((a) => {
    a.href = location.protocol + '//' + location.hostname + ':' + a.dataset.port + a.getAttribute('data-path');
  });

  // 2) SH-02: đổi variant → cập nhật giá
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

  // 5) Form có data-confirm → hỏi lại trước khi gửi
  document.querySelectorAll('form[data-confirm]').forEach((f) =>
    f.addEventListener('submit', (e) => { if (!confirm(f.dataset.confirm)) e.preventDefault(); }));
})();
