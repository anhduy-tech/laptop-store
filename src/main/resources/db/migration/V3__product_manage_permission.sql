-- V3: quyền quản lý thông tin sản phẩm (xem/sửa product + variant, KHÔNG gồm giá: giá đi qua đề xuất -> duyệt, BR-06)
INSERT INTO permission (code, name, module, portal) VALUES ('PRODUCT_MANAGE', 'Sửa thông tin sản phẩm', 'catalog', 'warehouse');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.code = 'PRODUCT_MANAGE'
WHERE r.code IN ('WAREHOUSE_PROMO', 'ADMIN');
