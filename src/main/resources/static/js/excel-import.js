/* "Nhập từ Excel": bấm nút có data-excel-target (CSS selector của ô textarea, hoặc "closest" = ô textarea cùng ô bảng),
 * chọn file .xlsx/.xls; server đọc cột Serial (hoặc cột A) và trả mỗi serial một dòng, JS gộp vào ô textarea (bỏ trùng). */
(function () {
  'use strict';
  var URL_PARSE = '/staff/warehouse/excel/serials';

  function csrf() {
    var t = document.querySelector('meta[name="_csrf"]');
    var h = document.querySelector('meta[name="_csrf_header"]');
    return t && h && t.content && h.content ? [h.content, t.content] : null;
  }

  function targetOf(btn) {
    var sel = btn.dataset.excelTarget;
    if (sel === 'closest') {
      var holder = btn.closest('td') || btn.parentElement;
      return holder ? holder.querySelector('textarea') : null;
    }
    return document.querySelector(sel);
  }

  /** Gộp dòng mới vào nội dung cũ, giữ thứ tự và bỏ trùng; trả số dòng thực sự thêm. */
  function merge(textarea, lines) {
    var seen = {};
    var out = [];
    textarea.value.split(/\r?\n/).forEach(function (l) {
      l = l.trim();
      if (l && !seen[l]) { seen[l] = true; out.push(l); }
    });
    var before = out.length;
    lines.forEach(function (l) {
      l = l.trim();
      if (l && !seen[l]) { seen[l] = true; out.push(l); }
    });
    textarea.value = out.join('\n');
    textarea.dispatchEvent(new Event('input', { bubbles: true })); // để receipt.js cập nhật số máy
    return out.length - before;
  }

  function note(btn, text, isError) {
    var holder = btn.parentElement;
    var el = holder.querySelector('.excel-note');
    if (!el) {
      el = document.createElement('span');
      el.className = 'excel-note mt-1 block max-w-xs text-xs font-medium';
      btn.insertAdjacentElement('afterend', el);
    }
    el.textContent = text;
    el.classList.toggle('text-red-600', !!isError);
    el.classList.toggle('text-emerald-700', !isError);
  }

  function upload(file, btn, textarea) {
    var fd = new FormData();
    fd.append('file', file);
    var headers = {};
    var c = csrf();
    if (c) headers[c[0]] = c[1];
    var label = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Đang đọc...';
    fetch(URL_PARSE, { method: 'POST', body: fd, headers: headers, credentials: 'same-origin' })
      .then(function (res) { return res.text().then(function (t) { return { ok: res.ok, text: t }; }); })
      .then(function (r) {
        if (!r.ok) { note(btn, r.text || 'Không đọc được file.', true); return; }
        var lines = r.text.split('\n').filter(Boolean);
        var added = merge(textarea, lines);
        note(btn, 'Đọc ' + lines.length + ' serial từ ' + file.name + (added < lines.length ? ' (' + (lines.length - added) + ' dòng đã có sẵn)' : '') + '.', false);
      })
      .catch(function () { note(btn, 'Không gửi được file, hãy thử lại.', true); })
      .then(function () { btn.disabled = false; btn.textContent = label; });
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-excel-target]');
    if (!btn) return;
    e.preventDefault();
    var textarea = targetOf(btn);
    if (!textarea) return;
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = '.xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel';
    input.addEventListener('change', function () {
      if (input.files && input.files.length) upload(input.files[0], btn, textarea);
    });
    input.click();
  });

  if (typeof window !== 'undefined') window.ExcelImport = { merge: merge };
})();
