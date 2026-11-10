/* WH-01 nhập kho: chọn/tạo sản phẩm, nhân tổ hợp thuộc tính thành biến thể, mỗi biến thể có giá vốn, giá bán, serial.
 * CPU / GPU / RAM / ổ cứng chọn từ danh mục thuộc tính (ô tick), không gõ tay; muốn thêm giá trị mới thì nhập vào ô "Thêm"
 * và hệ thống tự kiểm tra trùng: trùng thì chọn luôn giá trị đã có. GPU không bắt buộc (không chọn = không có GPU rời).
 * Trạng thái nằm trong `state` (một nguồn sự thật). Khi gửi form, sinh các trường products[i].variants[j].* để Spring bind.
 * Phần logic thuần (ReceiptLogic) không đụng DOM nên chạy thử được bằng Node. */
(function (root) {
  'use strict';

  var MAX_ROWS = 200;
  var NEW_BRAND = '__new__';
  var DRAFT_KEY = 'lapstore.receipt.draft';
  var KINDS = ['cpu', 'gpu', 'ram', 'storage'];
  var KIND_LABEL = { cpu: 'CPU', gpu: 'GPU', ram: 'RAM', storage: 'Ổ cứng' };
  var FIELD = { cpu: 'cpus', gpu: 'gpus', ram: 'rams', storage: 'storages' };
  var NUMERIC = { ram: true, storage: true };

  // ------------------------------------------------------------------ logic thuần

  /** Khóa chuẩn hóa giống server: bỏ dấu, chữ thường, chỉ giữ chữ và số ("Core i5-1335U" = "core i5 1335u"). */
  function normKey(s) {
    return String(s == null ? '' : s).replace(/đ/g, 'd').replace(/Đ/g, 'D').normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
  }

  /** "16" -> "16", "16GB" -> "16", "1TB" -> "1024"; không hợp lệ -> null. */
  function parseGb(raw) {
    var m = /^\s*(\d{1,6})\s*(gb|g|tb|t)?\s*$/i.exec(String(raw == null ? '' : raw));
    if (!m) return null;
    var n = Number(m[1]);
    if (m[2] && m[2].toLowerCase().charAt(0) === 't') n *= 1024;
    return n > 0 && n <= 99999 ? String(n) : null;
  }

  function formatGb(n) {
    n = Number(n);
    return n >= 1024 && n % 1024 === 0 ? (n / 1024) + ' TB' : n + ' GB';
  }

  function cleanText(s) { return String(s == null ? '' : s).trim().replace(/\s+/g, ' '); }

  /**
   * Thêm giá trị vào danh mục `list` (mảng {value,label,isNew}) nếu chưa có.
   * @return {status:'added'|'exists'|'invalid', option, message}
   */
  function addOption(list, kind, raw) {
    var value;
    var label;
    if (NUMERIC[kind]) {
      value = parseGb(raw);
      if (!value) return { status: 'invalid', message: 'Nhập số GB hợp lệ cho ' + KIND_LABEL[kind] + ' (VD 16, 512, 1TB).' };
      label = formatGb(value);
    } else {
      value = cleanText(raw);
      if (!value) return { status: 'invalid', message: 'Nhập giá trị ' + KIND_LABEL[kind] + '.' };
      if (value.length > 100) return { status: 'invalid', message: KIND_LABEL[kind] + ' tối đa 100 ký tự.' };
      label = value;
    }
    var key = NUMERIC[kind] ? value : normKey(value);
    for (var i = 0; i < list.length; i++) {
      var existing = NUMERIC[kind] ? list[i].value : normKey(list[i].value);
      if (existing === key) {
        return { status: 'exists', option: list[i], message: 'Đã có "' + list[i].label + '", đã chọn giá trị đó.' };
      }
    }
    var opt = { value: value, label: label, isNew: true };
    list.push(opt);
    return { status: 'added', option: opt, message: 'Đã thêm "' + label + '" (được lưu khi xác nhận nhập kho).' };
  }

  function cartesian(arrays) {
    return arrays.reduce(function (acc, arr) {
      var out = [];
      acc.forEach(function (a) { arr.forEach(function (x) { out.push(a.concat([x])); }); });
      return out;
    }, [[]]);
  }

  function rowKey(r) { return [r.colorId, r.cpu, r.gpu, r.ram, r.storage].join('|'); }

  function splitLines(text) {
    return String(text == null ? '' : text).split(/\r?\n/).map(function (s) { return s.trim(); })
      .filter(function (s) { return s.length > 0; });
  }

  function countSerials(text) { return splitLines(text).length; }

  /** Số giá trị của từng trục (GPU trống tính là 1: không có GPU rời). */
  function axisCounts(block) {
    return { colors: block.colorIds.length, cpus: block.cpus.length, gpus: block.gpus.length || 1,
      rams: block.rams.length, storages: block.storages.length };
  }

  /** Nhân tổ hợp màu x CPU x GPU x RAM x ổ cứng; giữ lại giá/serial của dòng cũ có cùng tổ hợp. */
  function generateRows(block, oldRows) {
    var gpus = block.gpus.length ? block.gpus : [''];
    if (!block.colorIds.length || !block.cpus.length || !block.rams.length || !block.storages.length) {
      return { rows: [], total: 0, truncated: false };
    }
    var combos = cartesian([block.colorIds, block.cpus, gpus, block.rams, block.storages]);
    var byKey = {};
    (oldRows || []).forEach(function (r) { byKey[rowKey(r)] = r; });
    var rows = combos.slice(0, MAX_ROWS).map(function (c) {
      var row = { colorId: c[0], cpu: c[1], gpu: c[2], ram: c[3], storage: c[4], unitCost: '', sellPrice: '', serials: '' };
      var old = byKey[rowKey(row)];
      if (old) { row.unitCost = old.unitCost; row.sellPrice = old.sellPrice; row.serials = old.serials; }
      return row;
    });
    return { rows: rows, total: combos.length, truncated: combos.length > MAX_ROWS };
  }

  /** Các serial xuất hiện từ hai lần trở lên trong toàn phiếu. */
  function findDuplicates(state) {
    var seen = {};
    var dups = [];
    state.blocks.forEach(function (b) {
      b.rows.forEach(function (r) {
        splitLines(r.serials).forEach(function (s) {
          if (seen[s] === 1) dups.push(s);
          seen[s] = (seen[s] || 0) + 1;
        });
      });
    });
    return dups;
  }

  function rowsWithSerials(block) {
    return block.rows.filter(function (r) { return countSerials(r.serials) > 0; });
  }

  function describeRow(r) {
    return r.cpu + (r.gpu ? ' / ' + r.gpu : '') + ' / ' + formatGb(r.ram) + ' / ' + formatGb(r.storage);
  }

  /** @param isExisting function(block,row) -> true nếu biến thể đã có (không cần giá bán). Trả '' nếu hợp lệ. */
  function validateState(state, isExisting) {
    var total = 0;
    for (var i = 0; i < state.blocks.length; i++) {
      var b = state.blocks[i];
      var rows = rowsWithSerials(b);
      if (!rows.length) continue;
      total += rows.length;
      var where = 'Sản phẩm ' + (i + 1) + ': ';
      if (b.mode === 'existing') {
        if (!b.productId) return where + 'chọn sản phẩm có sẵn, hoặc chuyển sang "Tạo sản phẩm mới".';
      } else {
        if (!b.name || !b.name.trim()) return where + 'nhập tên sản phẩm mới.';
        if (!b.categoryId) return where + 'chọn danh mục.';
        if (!b.brandId) return where + 'chọn hãng.';
        if (b.brandId === NEW_BRAND && !(b.newBrandName && b.newBrandName.trim())) return where + 'nhập tên hãng mới.';
      }
      for (var j = 0; j < rows.length; j++) {
        var r = rows[j];
        var label = where + 'cấu hình ' + describeRow(r) + ': ';
        if (!(Number(r.unitCost) > 0)) return label + 'nhập giá vốn lớn hơn 0.';
        if (!isExisting(b, r) && !(Number(r.sellPrice) > 0)) return label + 'đây là biến thể mới, nhập giá bán lớn hơn 0.';
      }
    }
    if (!total) return 'Chưa có serial nào. Nhập serial ở ô Serial của từng cấu hình (mỗi dòng một serial).';
    var dups = findDuplicates(state);
    if (dups.length) return 'Serial bị lặp trong phiếu: ' + dups.slice(0, 5).join(', ') + (dups.length > 5 ? '...' : '');
    return '';
  }

  /** Danh sách [tên trường, giá trị] cho Spring bind. Bỏ qua sản phẩm/dòng không có serial và đánh lại chỉ số liên tục. */
  function toFormEntries(state) {
    var out = [];
    var bi = 0;
    state.blocks.forEach(function (b) {
      var rows = rowsWithSerials(b);
      if (!rows.length) return;
      var p = 'products[' + bi + '].';
      if (b.mode === 'existing') {
        out.push([p + 'productId', b.productId]);
      } else {
        if (b.brandId === NEW_BRAND) out.push([p + 'newBrandName', b.newBrandName]);
        else out.push([p + 'brandId', b.brandId]);
        out.push([p + 'categoryId', b.categoryId]);
        out.push([p + 'name', b.name]);
        out.push([p + 'series', b.series]);
        out.push([p + 'releaseYear', b.releaseYear]);
        out.push([p + 'description', b.description]);
      }
      rows.forEach(function (r, vi) {
        var v = p + 'variants[' + vi + '].';
        out.push([v + 'colorId', r.colorId]);
        out.push([v + 'cpu', r.cpu]);
        out.push([v + 'gpu', r.gpu]);
        out.push([v + 'ramGb', r.ram]);
        out.push([v + 'storageGb', r.storage]);
        out.push([v + 'unitCost', r.unitCost]);
        if (r.sellPrice !== '') out.push([v + 'sellPrice', r.sellPrice]);
        out.push([v + 'serials', splitLines(r.serials).join('\n')]);
      });
      bi++;
    });
    return out;
  }

  function newBlock() {
    return { mode: 'existing', productId: '', brandId: '', newBrandName: '', categoryId: '', name: '', series: '',
      releaseYear: '', description: '', colorIds: [], cpus: [], gpus: [], rams: [], storages: [],
      bulkCost: '', bulkSell: '', rows: [], total: 0, truncated: false };
  }

  root.ReceiptLogic = { normKey: normKey, parseGb: parseGb, formatGb: formatGb, addOption: addOption,
    cartesian: cartesian, rowKey: rowKey, splitLines: splitLines, countSerials: countSerials,
    axisCounts: axisCounts, generateRows: generateRows, findDuplicates: findDuplicates,
    validateState: validateState, toFormEntries: toFormEntries, newBlock: newBlock, MAX_ROWS: MAX_ROWS };

  if (typeof document === 'undefined') return;

  // ------------------------------------------------------------------ giao diện

  function init() {
    var form = document.getElementById('receiptForm');
    if (!form) return;
    var $ = function (sel, ctx) { return (ctx || document).querySelector(sel); };
    var blocksEl = $('#blocks');
    var tplBlock = $('#tplBlock');
    var tplRow = $('#tplRow');
    var tplChip = $('#tplChip');
    var uid = 0;

    // ---- dữ liệu danh mục do server render sẵn trong trang
    function optionsOf(id) {
      return Array.prototype.map.call($(id).options, function (o) { return { value: o.value, text: o.text }; });
    }
    var brands = optionsOf('#dataBrands');
    var categories = optionsOf('#dataCategories');
    var products = optionsOf('#dataProducts');
    var colors = Array.prototype.map.call(document.querySelectorAll('#dataColors li'), function (li) {
      return { id: li.dataset.id, name: li.dataset.name, hex: li.dataset.hex || '' };
    });
    var colorById = {};
    colors.forEach(function (c) { colorById[c.id] = c; });

    var specOptions = { cpu: [], gpu: [], ram: [], storage: [] };
    Array.prototype.forEach.call(document.querySelectorAll('#dataSpecs li'), function (li) {
      specOptions[li.dataset.kind].push({ value: li.dataset.value, label: li.dataset.label, isNew: false });
    });

    var variantIndex = {};
    Array.prototype.forEach.call(document.querySelectorAll('#dataVariants li'), function (li) {
      var d = li.dataset;
      var key = [d.product, d.color, normKey(d.cpu), normKey(d.gpu || ''), d.ram, d.storage].join('|');
      variantIndex[key] = { sku: d.sku, status: d.status };
    });

    function findExisting(b, r) {
      if (b.mode !== 'existing' || !b.productId) return null;
      return variantIndex[[b.productId, r.colorId, normKey(r.cpu), normKey(r.gpu), r.ram, r.storage].join('|')] || null;
    }

    function labelOf(kind, value) {
      if (kind === 'gpu' && value === '') return 'Không có';
      var list = specOptions[kind];
      for (var i = 0; i < list.length; i++) if (list[i].value === value) return list[i].label;
      return NUMERIC[kind] ? formatGb(value) : value;
    }

    var state = { blocks: [] };

    function fillSelect(sel, opts, placeholder, extra) {
      sel.innerHTML = '';
      var first = document.createElement('option');
      first.value = '';
      first.textContent = placeholder;
      sel.appendChild(first);
      opts.forEach(function (o) {
        var op = document.createElement('option');
        op.value = o.value;
        op.textContent = o.text;
        sel.appendChild(op);
      });
      if (extra) {
        var ex = document.createElement('option');
        ex.value = extra.value;
        ex.textContent = extra.text;
        sel.appendChild(ex);
      }
    }

    function toggleBoxes(el, b) {
      $('[data-role=existingBox]', el).hidden = b.mode !== 'existing';
      $('[data-role=newBox]', el).hidden = b.mode !== 'new';
      $('[data-role=newBrandBox]', el).hidden = b.brandId !== NEW_BRAND;
    }

    function makeChip(label, checked, attrs, swatch) {
      var chip = tplChip.content.cloneNode(true).querySelector('label');
      var cb = $('input', chip);
      Object.keys(attrs).forEach(function (k) { cb.dataset[k] = attrs[k]; });
      cb.checked = checked;
      $('[data-role=name]', chip).textContent = label;
      var sw = $('[data-role=swatch]', chip);
      if (swatch) sw.style.backgroundColor = swatch; else sw.hidden = true;
      return chip;
    }

    /** Dựng lại các ô tick CPU/GPU/RAM/ổ cứng của một sản phẩm từ danh mục hiện có. */
    function renderChips(el, b) {
      KINDS.forEach(function (kind) {
        var box = $('[data-role=chips-' + kind + ']', el);
        box.innerHTML = '';
        var list = specOptions[kind].slice();
        if (kind === 'gpu') list.unshift({ value: '', label: 'Không có', isNew: false });
        list.forEach(function (o) {
          var chip = makeChip(o.label + (o.isNew ? ' (mới)' : ''), b[FIELD[kind]].indexOf(o.value) >= 0,
            { spec: kind, val: o.value });
          box.appendChild(chip);
        });
        if (!list.length) box.textContent = 'Chưa có giá trị nào, hãy thêm ở ô bên dưới.';
      });
    }

    function buildBlockEl(b) {
      var el = tplBlock.content.cloneNode(true).querySelector('[data-block]');
      el._block = b;
      uid++;
      fillSelect($('[data-f=productId]', el), products, '— Chọn sản phẩm —');
      fillSelect($('[data-f=brandId]', el), brands, '— Chọn hãng —', { value: NEW_BRAND, text: '+ Hãng mới...' });
      fillSelect($('[data-f=categoryId]', el), categories, '— Chọn danh mục —');
      var chips = $('[data-role=colors]', el);
      colors.forEach(function (c) {
        chips.appendChild(makeChip(c.name, b.colorIds.indexOf(c.id) >= 0, { chip: c.id }, c.hex));
      });
      renderChips(el, b);
      Array.prototype.forEach.call(el.querySelectorAll('[data-f]'), function (inp) {
        var f = inp.dataset.f;
        if (inp.type === 'radio') {
          inp.name = 'mode_' + uid;
          inp.checked = inp.value === b.mode;
        } else if (f in b) {
          inp.value = b[f] == null ? '' : b[f];
        }
      });
      toggleBoxes(el, b);
      return el;
    }

    function renderRows(b, el) {
      var tbody = $('[data-role=rows]', el);
      tbody.innerHTML = '';
      $('[data-role=tableWrap]', el).hidden = b.rows.length === 0;
      $('[data-role=bulkBox]', el).hidden = b.rows.length === 0;
      var ax = axisCounts(b);
      var info = $('[data-role=comboInfo]', el);
      if (!b.rows.length) {
        info.textContent = 'Tick ít nhất một màu, CPU, RAM và ổ cứng để sinh các biến thể (GPU không bắt buộc).';
      } else {
        info.textContent = ax.colors + ' màu × ' + ax.cpus + ' CPU × ' + ax.gpus + ' GPU × ' + ax.rams + ' RAM × ' +
          ax.storages + ' ổ cứng = ' + b.total + ' biến thể' +
          (b.truncated ? ' (chỉ hiện ' + MAX_ROWS + ' dòng đầu, hãy bớt giá trị)' : '') + '.';
      }
      b.rows.forEach(function (r, i) {
        var tr = tplRow.content.cloneNode(true).querySelector('tr');
        tr.dataset.row = i;
        var color = colorById[r.colorId];
        $('[data-role=color]', tr).textContent = color ? color.name : r.colorId;
        $('[data-role=cpu]', tr).textContent = r.cpu;
        $('[data-role=gpu]', tr).textContent = labelOf('gpu', r.gpu);
        $('[data-role=ram]', tr).textContent = labelOf('ram', r.ram);
        $('[data-role=storage]', tr).textContent = labelOf('storage', r.storage);
        var ex = findExisting(b, r);
        var st = $('[data-role=status]', tr);
        if (ex) {
          st.textContent = 'Đã có · ' + ex.sku + (ex.status === 'INACTIVE' ? ' (chờ duyệt giá)' : '');
          st.className = 'badge badge-green';
        } else {
          st.textContent = 'Biến thể mới';
          st.className = 'badge badge-amber';
        }
        var cost = $('[data-f=unitCost]', tr);
        var sell = $('[data-f=sellPrice]', tr);
        cost.value = r.unitCost;
        if (ex) { sell.disabled = true; sell.value = ''; sell.placeholder = 'Giữ giá hiện tại'; } else { sell.value = r.sellPrice; }
        $('[data-f=serials]', tr).value = r.serials;
        updateRowCount(tr, r);
        tbody.appendChild(tr);
      });
    }

    function updateRowCount(tr, r) {
      var n = countSerials(r.serials);
      $('[data-role=count]', tr).textContent = n ? n + ' máy' : 'bỏ qua nếu để trống';
    }

    function regenerate(b, el) {
      var g = generateRows(b, b.rows);
      b.rows = g.rows;
      b.total = g.total;
      b.truncated = g.truncated;
      renderRows(b, el);
      updateSummary();
    }

    function updateSummary() {
      var serials = 0;
      var rows = 0;
      state.blocks.forEach(function (b) {
        b.rows.forEach(function (r) {
          var n = countSerials(r.serials);
          if (n) { serials += n; rows++; }
        });
      });
      var touched = state.blocks.some(function (b) {
        return b.colorIds.length || b.cpus.length || b.gpus.length || b.rams.length || b.storages.length ||
          b.productId || (b.name && b.name.trim());
      });
      // workspace.js đọc cờ này để cảnh báo khi đóng tab đang nhập dở
      document.documentElement.dataset.dirty = (serials > 0 || touched) ? '1' : '0';
      $('#sumSerials').textContent = serials;
      $('#sumRows').textContent = rows;
      var dups = findDuplicates(state);
      var warn = $('#dupWarn');
      warn.hidden = dups.length === 0;
      warn.textContent = dups.length ? 'Serial bị lặp: ' + dups.slice(0, 5).join(', ') + (dups.length > 5 ? '...' : '') : '';
    }

    function renumber() {
      Array.prototype.forEach.call(blocksEl.children, function (el, i) { $('[data-role=no]', el).textContent = i + 1; });
    }

    function addBlock(initial) {
      var b = initial ? Object.assign(newBlock(), initial) : newBlock();
      state.blocks.push(b);
      var el = buildBlockEl(b);
      blocksEl.appendChild(el);
      renumber();
      renderRows(b, el);
      updateSummary();
    }

    function toggleValue(b, kind, value, on) {
      var arr = b[FIELD[kind]];
      var idx = arr.indexOf(value);
      if (on && idx < 0) arr.push(value);
      if (!on && idx >= 0) arr.splice(idx, 1);
      var order = specOptions[kind].map(function (o) { return o.value; });
      if (kind === 'gpu') order.unshift('');
      arr.sort(function (x, y) { return order.indexOf(x) - order.indexOf(y); });
    }

    // ---- sự kiện
    blocksEl.addEventListener('input', function (e) {
      var t = e.target;
      var f = t.dataset.f;
      if (!f || t.type === 'radio') return;
      var el = t.closest('[data-block]');
      var b = el._block;
      var tr = t.closest('tr[data-row]');
      if (tr) {
        var r = b.rows[Number(tr.dataset.row)];
        r[f] = t.value;
        if (f === 'serials') { updateRowCount(tr, r); updateSummary(); }
        return;
      }
      b[f] = t.value;
    });

    blocksEl.addEventListener('change', function (e) {
      var t = e.target;
      var el = t.closest('[data-block]');
      if (!el) return;
      var b = el._block;
      if (t.dataset.chip) {
        var id = t.dataset.chip;
        var idx = b.colorIds.indexOf(id);
        if (t.checked && idx < 0) b.colorIds.push(id);
        if (!t.checked && idx >= 0) b.colorIds.splice(idx, 1);
        b.colorIds.sort(function (x, y) { return colors.findIndex(function (c) { return c.id === x; }) - colors.findIndex(function (c) { return c.id === y; }); });
        regenerate(b, el);
        return;
      }
      if (t.dataset.spec) {
        toggleValue(b, t.dataset.spec, t.dataset.val, t.checked);
        regenerate(b, el);
        return;
      }
      var f = t.dataset.f;
      if (!f || t.closest('tr[data-row]')) return;
      if (f === 'mode') {
        if (t.checked) { b.mode = t.value; toggleBoxes(el, b); renderRows(b, el); }
      } else if (f === 'productId') {
        b.productId = t.value;
        renderRows(b, el);
        updateSummary();
      } else if (f === 'brandId') {
        b.brandId = t.value;
        toggleBoxes(el, b);
      }
    });

    /** Thêm giá trị thuộc tính mới ngay trong khối; trùng thì chọn luôn giá trị đã có. */
    function addSpec(el, kind) {
      var b = el._block;
      var input = $('[data-add=' + kind + ']', el);
      var note = $('[data-role=specNote]', el);
      var res = addOption(specOptions[kind], kind, input.value);
      note.textContent = res.message;
      note.className = 'mt-2 text-sm font-medium ' + (res.status === 'invalid' ? 'text-red-600' : 'text-emerald-700');
      if (res.status === 'invalid') return;
      toggleValue(b, kind, res.option.value, true);
      input.value = '';
      Array.prototype.forEach.call(blocksEl.children, function (other) { renderChips(other, other._block); });
      regenerate(b, el);
    }

    blocksEl.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && e.target.dataset.add) {
        e.preventDefault();
        addSpec(e.target.closest('[data-block]'), e.target.dataset.add);
      }
    });

    blocksEl.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-act]');
      if (!btn) return;
      var el = btn.closest('[data-block]');
      var b = el._block;
      if (btn.dataset.act === 'removeBlock') {
        if (state.blocks.length === 1) {
          state.blocks[0] = newBlock();
          var fresh = buildBlockEl(state.blocks[0]);
          blocksEl.replaceChild(fresh, el);
          renderRows(state.blocks[0], fresh);
        } else {
          state.blocks.splice(state.blocks.indexOf(b), 1);
          blocksEl.removeChild(el);
        }
        renumber();
        updateSummary();
      } else if (btn.dataset.act === 'applyBulk') {
        b.rows.forEach(function (r) {
          if (b.bulkCost !== '') r.unitCost = b.bulkCost;
          if (b.bulkSell !== '' && !findExisting(b, r)) r.sellPrice = b.bulkSell;
        });
        renderRows(b, el);
      } else if (btn.dataset.act === 'addSpec') {
        addSpec(el, btn.dataset.kind);
      }
    });

    $('#addBlock').addEventListener('click', function () { addBlock(); });

    function showError(msg) {
      var box = $('#clientError');
      box.textContent = msg;
      box.hidden = false;
      box.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    form.addEventListener('submit', function (e) {
      $('#clientError').hidden = true;
      var err = validateState(state, findExisting);
      if (err) { e.preventDefault(); showError(err); return; }
      var hf = $('#hiddenFields');
      hf.innerHTML = '';
      toFormEntries(state).forEach(function (kv) {
        var ta = document.createElement('textarea');
        ta.hidden = true;
        ta.name = kv[0];
        ta.value = kv[1] == null ? '' : kv[1];
        hf.appendChild(ta);
      });
      try {
        var newOpts = {};
        KINDS.forEach(function (k) { newOpts[k] = specOptions[k].filter(function (o) { return o.isNew; }); });
        sessionStorage.setItem(DRAFT_KEY, JSON.stringify({
          supplierId: form.elements.supplierId.value, branchId: form.elements.branchId.value,
          note: form.elements.note.value, newOptions: newOpts, blocks: state.blocks }));
      } catch (x) { /* không lưu được bản nháp thì thôi */ }
    });

    // ---- khôi phục khi server trả lỗi (redirect về trang này kèm thông báo lỗi)
    var restored = false;
    try {
      var raw = sessionStorage.getItem(DRAFT_KEY);
      if (raw && document.querySelector('.alert-error')) {
        var d = JSON.parse(raw);
        ['supplierId', 'branchId', 'note'].forEach(function (k) { if (d[k] != null) form.elements[k].value = d[k]; });
        KINDS.forEach(function (k) {
          ((d.newOptions || {})[k] || []).forEach(function (o) { addOption(specOptions[k], k, o.value); });
        });
        (d.blocks || []).forEach(function (b) { addBlock(b); });
        restored = state.blocks.length > 0;
      }
      if (!restored) sessionStorage.removeItem(DRAFT_KEY);
    } catch (x) { /* bản nháp hỏng: bỏ qua */ }
    if (!restored) addBlock();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})(typeof window !== 'undefined' ? window : globalThis);
