// Chạy: node src/test/js/receipt.test.js (logic thuần của màn nhập kho, không cần trình duyệt)
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const ctx = {}; ctx.globalThis = ctx; vm.createContext(ctx);
vm.runInContext(fs.readFileSync(require('path').join(__dirname, '../../main/resources/static/js/receipt.js'), 'utf8'), ctx);
const L = ctx.ReceiptLogic;
const arr = x => Array.from(x);

// --- chuẩn hóa / chống trùng
assert.strictEqual(L.normKey('Core i5-1335U'), L.normKey('core i5 1335u'));
assert.strictEqual(L.normKey('Không có'), 'khongco');
assert.notStrictEqual(L.normKey('RTX 4050'), L.normKey('RTX 4060'));
assert.strictEqual(L.parseGb('16'), '16'); assert.strictEqual(L.parseGb('16GB'), '16'); assert.strictEqual(L.parseGb(' 1 TB '), '1024');
assert.strictEqual(L.parseGb('abc'), null); assert.strictEqual(L.parseGb('0'), null);
assert.strictEqual(L.formatGb('1024'), '1 TB'); assert.strictEqual(L.formatGb('512'), '512 GB'); assert.strictEqual(L.formatGb('1536'), '1536 GB');

// --- addOption: trùng thì chọn cái đã có, không tạo thêm
let cpus = [{ value: 'Intel Core i5-1335U', label: 'Intel Core i5-1335U', isNew: false }];
let r = L.addOption(cpus, 'cpu', 'intel core i5 1335u');
assert.strictEqual(r.status, 'exists'); assert.strictEqual(r.option.value, 'Intel Core i5-1335U'); assert.strictEqual(cpus.length, 1);
r = L.addOption(cpus, 'cpu', '  Intel   Core i7-1355U '); assert.strictEqual(r.status, 'added'); assert.strictEqual(cpus.length, 2);
assert.strictEqual(cpus[1].value, 'Intel Core i7-1355U'); assert.ok(cpus[1].isNew);
assert.strictEqual(L.addOption(cpus, 'cpu', '   ').status, 'invalid');
let rams = [{ value: '16', label: '16 GB', isNew: false }];
assert.strictEqual(L.addOption(rams, 'ram', '16GB').status, 'exists'); assert.strictEqual(rams.length, 1);
assert.strictEqual(L.addOption(rams, 'ram', 'x').status, 'invalid');
let sto = [{ value: '1024', label: '1 TB', isNew: false }];
assert.strictEqual(L.addOption(sto, 'storage', '1TB').status, 'exists');
assert.strictEqual(L.addOption(sto, 'storage', '2TB').option.value, '2048');

// --- 2 màu x 3 RAM x 3 ổ cứng = 18 (GPU không chọn = 1 giá trị "không có")
let b = L.newBlock(); b.colorIds = ['1', '2']; b.cpus = ['i5']; b.rams = ['8', '16', '32']; b.storages = ['256', '512', '1024'];
let g = L.generateRows(b, []); assert.strictEqual(g.total, 18); assert.strictEqual(g.rows.length, 18);
assert.ok(g.rows.every(x => x.gpu === ''));
// --- thêm trục GPU: 2 màu x 2 GPU = 4 (1 CPU, 1 RAM, 1 ổ)
b = L.newBlock(); b.colorIds = ['1', '2']; b.cpus = ['i7']; b.gpus = ['RTX 4050', 'RTX 4060']; b.rams = ['16']; b.storages = ['512'];
g = L.generateRows(b, []); assert.strictEqual(g.total, 4);
assert.deepStrictEqual(arr(new Set(g.rows.map(x => x.gpu))), ['RTX 4050', 'RTX 4060']);
// GPU: có hoặc không có => 2 biến thể
b.gpus = ['', 'RTX 4050']; b.colorIds = ['1']; assert.strictEqual(L.generateRows(b, []).total, 2);
// giữ dữ liệu khi sinh lại
b = L.newBlock(); b.colorIds = ['1']; b.cpus = ['i5']; b.rams = ['8', '16']; b.storages = ['512'];
g = L.generateRows(b, []); g.rows[0].serials = 'A1\nA2'; g.rows[0].unitCost = '100'; b.rows = g.rows;
b.rams = ['8', '16', '32']; let g2 = L.generateRows(b, b.rows);
assert.strictEqual(g2.total, 3); assert.strictEqual(g2.rows.find(x => x.serials === 'A1\nA2').unitCost, '100');
// thiếu trục bắt buộc -> 0
b.cpus = []; assert.strictEqual(L.generateRows(b, []).total, 0);
// giới hạn dòng
b = L.newBlock(); b.colorIds = ['1', '2', '3', '4', '5']; b.cpus = ['a', 'b', 'c', 'd', 'e']; b.rams = ['1', '2', '3', '4', '5']; b.storages = ['1', '2', '3', '4', '5'];
g = L.generateRows(b, []); assert.strictEqual(g.total, 625); assert.strictEqual(g.rows.length, 200); assert.ok(g.truncated);
assert.strictEqual(L.countSerials(' A\n\n B \r\nC'), 3);

// --- form entries + validate
const st = { blocks: [] }; let p = L.newBlock(); p.mode = 'existing'; p.productId = '7'; p.colorIds = ['1']; p.cpus = ['i5']; p.gpus = ['RTX 4050']; p.rams = ['16']; p.storages = ['512'];
p.rows = L.generateRows(p, []).rows; st.blocks.push(p);
assert.ok(L.validateState(st, () => false).startsWith('Chưa có serial'));
p.rows[0].serials = 'S1\nS2'; assert.ok(L.validateState(st, () => false).includes('giá vốn'));
p.rows[0].unitCost = '1000'; assert.ok(L.validateState(st, () => false).includes('giá bán'));
assert.strictEqual(L.validateState(st, () => true), '');
p.rows[0].sellPrice = '1500';
const m = Object.fromEntries(L.toFormEntries(st));
assert.strictEqual(m['products[0].productId'], '7'); assert.strictEqual(m['products[0].variants[0].ramGb'], '16');
assert.strictEqual(m['products[0].variants[0].gpu'], 'RTX 4050'); assert.strictEqual(m['products[0].variants[0].serials'], 'S1\nS2');
// serial trùng giữa 2 dòng
p.rows.push({ colorId: '1', cpu: 'i5', gpu: '', ram: '16', storage: '1024', unitCost: '1', sellPrice: '1', serials: 'S2' });
assert.deepStrictEqual(arr(L.findDuplicates(st)), ['S2']); assert.ok(L.validateState(st, () => true).includes('lặp'));
// sản phẩm mới + hãng mới, khối trống bị bỏ, chỉ số liên tục, GPU trống vẫn gửi
const st2 = { blocks: [L.newBlock(), L.newBlock()] };
let n = st2.blocks[1]; n.mode = 'new'; n.brandId = '__new__'; n.newBrandName = 'Lenovo'; n.categoryId = '3'; n.name = 'ThinkPad X1';
n.rows = [{ colorId: '2', cpu: 'i7', gpu: '', ram: '32', storage: '1024', unitCost: '5', sellPrice: '9', serials: 'Z1' }];
assert.strictEqual(L.validateState(st2, () => false), '');
const m2 = Object.fromEntries(L.toFormEntries(st2));
assert.strictEqual(m2['products[0].newBrandName'], 'Lenovo'); assert.ok(!('products[0].brandId' in m2));
assert.strictEqual(m2['products[0].variants[0].gpu'], '');
n.brandId = ''; assert.ok(L.validateState(st2, () => false).includes('chọn hãng'));
console.log('OK all receipt.js logic tests passed');
