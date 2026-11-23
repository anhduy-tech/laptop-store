-- V6: quyền nhả giữ máy thủ công (kho). Chỉ thêm dữ liệu, không đổi cấu trúc.
INSERT INTO permission (code, name, module, portal) VALUES ('STOCK_HOLD_RELEASE', 'Nhả giữ máy thủ công', 'inventory', 'warehouse');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.code = 'STOCK_HOLD_RELEASE'
WHERE r.code IN ('WAREHOUSE_PROMO', 'ADMIN');
