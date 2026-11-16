-- V5: quyền kiểm kê kho (tạo phiên, quét serial, đóng phiên, áp dụng điều chỉnh). Chỉ thêm dữ liệu, không đổi cấu trúc.
INSERT INTO permission (code, name, module, portal) VALUES ('STOCK_STOCKTAKE', 'Kiểm kê kho', 'inventory', 'warehouse');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.code = 'STOCK_STOCKTAKE'
WHERE r.code IN ('WAREHOUSE_PROMO', 'ADMIN');
