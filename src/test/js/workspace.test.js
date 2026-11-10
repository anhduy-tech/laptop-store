// Chạy: node src/test/js/workspace.test.js (logic tab thuần, không cần trình duyệt)
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const ctx = {}; ctx.globalThis = ctx; ctx.URL = URL; vm.createContext(ctx);
vm.runInContext(fs.readFileSync(require('path').join(__dirname, '../../main/resources/static/js/workspace.js'), 'utf8'), ctx);
const W = ctx.WorkspaceLogic;
const base = 'http://localhost:8083/staff/warehouse/receipts';
let n = 0; const id = () => 'id' + (++n);

// --- URL
assert.strictEqual(W.fullOf('/staff/warehouse/products?q=abc#x', base), '/staff/warehouse/products?q=abc');
assert.strictEqual(W.fullOf('http://localhost:8083/staff/warehouse/stock', base), '/staff/warehouse/stock');
assert.strictEqual(W.fullOf('http://other:9/x', base), 'http://other:9/x');
assert.strictEqual(W.pathOf('/staff/warehouse/products?q=abc', base), '/staff/warehouse/products');
assert.strictEqual(W.titleFrom('Nhập kho – Lapstore'), 'Nhập kho');
assert.strictEqual(W.titleFrom('Tồn kho - Lapstore Staff'), 'Tồn kho');
assert.strictEqual(W.titleFrom('Tồn kho'), 'Tồn kho');

// --- mở tab: tạo mới, kích hoạt lại tab cũ (giữ nguyên, không tạo trùng)
let tabs = [];
let r = W.openTab(tabs, '/staff/warehouse/receipts', 'Nhập kho', base, 3, id, false);
assert.strictEqual(r.status, 'created'); assert.strictEqual(tabs.length, 1);
const first = r.activeId;
r = W.openTab(tabs, '/staff/warehouse/transfers', 'Chuyển kho', base, 3, id, false);
assert.strictEqual(r.status, 'created'); assert.strictEqual(tabs.length, 2);
r = W.openTab(tabs, '/staff/warehouse/receipts', 'Nhập kho', base, 3, id, false);
assert.strictEqual(r.status, 'existing'); assert.strictEqual(r.activeId, first); assert.strictEqual(tabs.length, 2);
// đổi query của tab cũ chỉ khi navigate = true
r = W.openTab(tabs, '/staff/warehouse/receipts?x=1', 'Nhập kho', base, 3, id, false);
assert.strictEqual(tabs[0].url, '/staff/warehouse/receipts');
r = W.openTab(tabs, '/staff/warehouse/receipts?x=1', 'Nhập kho', base, 3, id, true);
assert.strictEqual(tabs[0].url, '/staff/warehouse/receipts?x=1');
// giới hạn số tab
W.openTab(tabs, '/staff/warehouse/stock', 'Tồn kho', base, 3, id, false);
r = W.openTab(tabs, '/staff/warehouse/attributes', 'Thuộc tính', base, 3, id, false);
assert.strictEqual(r.status, 'limit'); assert.strictEqual(tabs.length, 3);

// --- đóng tab: chuyển sang tab bên trái; tab đầu thì sang phải; tab không active thì giữ nguyên
const ids = tabs.map(t => t.id);
let c = W.closeTab(tabs.slice(), ids[2], ids[2]); assert.strictEqual(c.activeId, ids[1]);
c = W.closeTab(tabs.slice(), ids[0], ids[0]); assert.strictEqual(c.activeId, ids[1]);
c = W.closeTab(tabs.slice(), ids[1], ids[2]); assert.strictEqual(c.activeId, ids[1]); assert.strictEqual(c.tabs.length, 2);
c = W.closeTab([{ id: 'only', url: '/a', title: 'a' }], 'only', 'only'); assert.strictEqual(c.activeId, null);
// --- tab điều hướng sang trang đã có tab khác -> gộp
const list = [{ id: 'a', url: '/staff/warehouse/products', title: 'Sản phẩm' }, { id: 'b', url: '/staff/warehouse/products/5', title: 'SP: X' }];
assert.strictEqual(W.findDuplicateTab(list, list[1], base), null);
list[1].url = '/staff/warehouse/products?q=abc';          // tab chi tiết bấm "← Danh sách" (có query)
assert.strictEqual(W.findDuplicateTab(list, list[1], base).id, 'a');
list[1].url = '/staff/warehouse/products/5/edit';          // sang trang sửa: chưa trùng ai
assert.strictEqual(W.findDuplicateTab(list, list[1], base), null);
console.log('OK all workspace.js logic tests passed');
