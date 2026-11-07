
-- Chi nhánh
INSERT INTO branch (name, address, phone, branch_type)
SELECT 'Lapstore Hà Đông', '12 Nguyễn Trãi, Hà Đông, Hà Nội', '0243 111 0001', 'STORE' FROM dual
    UNION ALL SELECT 'Lapstore Cầu Giấy', '88 Cầu Giấy, Cầu Giấy, Hà Nội', '0243 111 0002', 'STORE' FROM dual
    UNION ALL SELECT 'Kho tổng Long Biên', 'KCN Sài Đồng, Long Biên, Hà Nội', '0243 111 0003', 'WAREHOUSE' FROM dual;

-- Nhà cung cấp
INSERT INTO supplier (name, tax_code, contact_name, phone, address)
SELECT 'FPT Trading', '0100000001', 'Nguyễn Hoài Nam', '0281110001', 'Hà Nội' FROM dual
    UNION ALL SELECT 'Synnex FPT', '0100000002', 'Trần Thu Hằng', '0281110002', 'TP. Hồ Chí Minh' FROM dual
    UNION ALL SELECT 'Digiworld', '0100000003', 'Lê Quang Huy', '0281110003', 'TP. Hồ Chí Minh' FROM dual;

-- Thương hiệu
INSERT INTO brand (name)
SELECT 'ASUS' FROM dual
    UNION ALL SELECT 'Apple' FROM dual
    UNION ALL SELECT 'Dell' FROM dual
    UNION ALL SELECT 'Lenovo' FROM dual
    UNION ALL SELECT 'HP' FROM dual
    UNION ALL SELECT 'Acer' FROM dual
    UNION ALL SELECT 'MSI' FROM dual;

-- Màu sắc
INSERT INTO color (name, hex_code)
SELECT 'Bạc', '#C0C0C0' FROM dual
    UNION ALL SELECT 'Đen', '#1F1F1F' FROM dual
    UNION ALL SELECT 'Xám không gian', '#6B7280' FROM dual
    UNION ALL SELECT 'Xanh đêm', '#1E3A5F' FROM dual;

-- Danh mục gốc
INSERT INTO category (parent_id, name, slug, sort_order)
SELECT NULL, 'Laptop', 'laptop', 1 FROM dual;

-- Danh mục con
INSERT INTO category (parent_id, name, slug, sort_order)
SELECT p.id, c.n, c.s, c.o FROM category p JOIN (
    SELECT 'Gaming' AS n, 'laptop-gaming' AS s, 1 AS o FROM dual
    UNION ALL SELECT 'Văn phòng', 'laptop-van-phong', 2 FROM dual
    UNION ALL SELECT 'Mỏng nhẹ', 'laptop-mong-nhe', 3 FROM dual
    UNION ALL SELECT 'Đồ họa', 'laptop-do-hoa', 4 FROM dual
) c ON 1 = 1 WHERE p.slug = 'laptop';

-- Sản phẩm
INSERT INTO product (brand_id, category_id, name, slug, series, release_year, description, status)
SELECT b.id, c.id, x.pname, x.pslug, x.series, x.yr, x.descr, 'ACTIVE'
FROM (
    SELECT 'ASUS ROG Strix G16 (2025)' AS pname, 'asus-rog-strix-g16-2025' AS pslug, 'ROG Strix' AS series, 2025 AS yr, 'Laptop gaming màn hình 16 inch 165Hz, tản nhiệt kép, bàn phím RGB. Phù hợp chơi game, đồ họa và làm việc nặng.' AS descr, 'ASUS' AS brand, 'laptop-gaming' AS cat FROM dual
    UNION ALL SELECT 'MSI Katana 15', 'msi-katana-15', 'Katana', 2024, 'Gaming tầm trung, hiệu năng ổn định, bàn phím đèn nền bốn vùng.', 'MSI', 'laptop-gaming' FROM dual
    UNION ALL SELECT 'Acer Nitro V 15', 'acer-nitro-v-15', 'Nitro', 2024, 'Gaming giá tốt cho học sinh, sinh viên, tản nhiệt hiệu quả.', 'Acer', 'laptop-gaming' FROM dual
    UNION ALL SELECT 'MacBook Air 13 M3', 'apple-macbook-air-13-m3', 'MacBook Air', 2024, 'Mỏng nhẹ, pin cả ngày, chip M3 mạnh mẽ và không quạt.', 'Apple', 'laptop-mong-nhe' FROM dual
    UNION ALL SELECT 'Dell XPS 13 Plus', 'dell-xps-13-plus', 'XPS', 2024, 'Thiết kế liền khối cao cấp, màn hình viền mỏng, bàn phím phẳng.', 'Dell', 'laptop-mong-nhe' FROM dual
    UNION ALL SELECT 'HP Spectre x360 14', 'hp-spectre-x360-14', 'Spectre', 2024, '2-trong-1 màn OLED cảm ứng, bút đi kèm, vỏ nhôm sang trọng.', 'HP', 'laptop-mong-nhe' FROM dual
    UNION ALL SELECT 'Acer Swift Go 14', 'acer-swift-go-14', 'Swift', 2024, 'Mỏng nhẹ 1.3 kg, màn OLED 2.8K, giá dễ tiếp cận.', 'Acer', 'laptop-mong-nhe' FROM dual
    UNION ALL SELECT 'ASUS Zenbook 14 OLED', 'asus-zenbook-14-oled', 'Zenbook', 2024, 'Màn OLED 3K 120Hz, chuẩn quân đội, pin bền.', 'ASUS', 'laptop-mong-nhe' FROM dual
    UNION ALL SELECT 'Lenovo ThinkPad X1 Carbon Gen 12', 'lenovo-thinkpad-x1-carbon-gen12', 'ThinkPad X1', 2024, 'Dòng doanh nhân siêu nhẹ, bàn phím tốt nhất phân khúc, bảo mật vân tay.', 'Lenovo', 'laptop-van-phong' FROM dual
    UNION ALL SELECT 'Lenovo ThinkPad E14 Gen 6', 'lenovo-thinkpad-e14-gen6', 'ThinkPad E', 2024, 'Bền bỉ cho văn phòng, nhiều cổng kết nối, dễ nâng cấp RAM và SSD.', 'Lenovo', 'laptop-van-phong' FROM dual
    UNION ALL SELECT 'HP EliteBook 840 G11', 'hp-elitebook-840-g11', 'EliteBook', 2024, 'Doanh nghiệp cao cấp, quản lý từ xa, vỏ magie.', 'HP', 'laptop-van-phong' FROM dual
    UNION ALL SELECT 'Dell Latitude 5450', 'dell-latitude-5450', 'Latitude', 2024, 'Ổn định cho doanh nghiệp, bảo hành tận nơi.', 'Dell', 'laptop-van-phong' FROM dual
    UNION ALL SELECT 'ASUS ProArt Studiobook 16', 'asus-proart-studiobook-16', 'ProArt', 2024, 'Máy trạm di động cho đồ họa 3D và dựng phim, màn hình 3.2K chuẩn màu.', 'ASUS', 'laptop-do-hoa' FROM dual
) x JOIN brand b ON b.name = x.brand JOIN category c ON c.slug = x.cat;

-- Variant (cấu hình)
INSERT INTO product_variant (product_id, sku, variant_name, color_id, cpu, gpu, ram_gb, storage_gb, current_price, warranty_months, status)
SELECT p.id, v.sku, v.vname, c.id, v.cpu, v.gpu, v.ram, v.ssd, v.price, 12, 'ACTIVE'
FROM (
    SELECT 'ROG-G16-I7-16-512' AS sku, 'asus-rog-strix-g16-2025' AS slug, 'i7 / 16GB / 512GB / RTX 4050' AS vname, 'Đen' AS color, 'Intel Core i7-14650HX' AS cpu, 'RTX 4050 6GB' AS gpu, 16 AS ram, 512 AS ssd, 33990000 AS price FROM dual
    UNION ALL SELECT 'ROG-G16-I7-16-1T', 'asus-rog-strix-g16-2025', 'i7 / 16GB / 1TB / RTX 4060', 'Đen', 'Intel Core i7-14650HX', 'RTX 4060 8GB', 16, 1024, 38990000 FROM dual
    UNION ALL SELECT 'KATANA15-I7-16-512', 'msi-katana-15', 'i7 / 16GB / 512GB / RTX 4050', 'Đen', 'Intel Core i7-13620H', 'RTX 4050 6GB', 16, 512, 26990000 FROM dual
    UNION ALL SELECT 'KATANA15-I5-8-512', 'msi-katana-15', 'i5 / 8GB / 512GB / RTX 3050', 'Đen', 'Intel Core i5-12450H', 'RTX 3050 4GB', 8, 512, 19990000 FROM dual
    UNION ALL SELECT 'NITROV15-I5-16-512', 'acer-nitro-v-15', 'i5 / 16GB / 512GB / RTX 4050', 'Đen', 'Intel Core i5-13420H', 'RTX 4050 6GB', 16, 512, 21990000 FROM dual
    UNION ALL SELECT 'MBA13-M3-8-256', 'apple-macbook-air-13-m3', 'M3 / 8GB / 256GB', 'Bạc', 'Apple M3', '10-core GPU', 8, 256, 27990000 FROM dual
    UNION ALL SELECT 'MBA13-M3-16-512', 'apple-macbook-air-13-m3', 'M3 / 16GB / 512GB', 'Xám không gian', 'Apple M3', '10-core GPU', 16, 512, 35990000 FROM dual
    UNION ALL SELECT 'XPS13-U7-16-512', 'dell-xps-13-plus', 'Ultra 7 / 16GB / 512GB', 'Bạc', 'Intel Core Ultra 7 155H', 'Intel Arc', 16, 512, 32500000 FROM dual
    UNION ALL SELECT 'XPS13-U7-32-1T', 'dell-xps-13-plus', 'Ultra 7 / 32GB / 1TB', 'Bạc', 'Intel Core Ultra 7 155H', 'Intel Arc', 32, 1024, 41500000 FROM dual
    UNION ALL SELECT 'SPECTRE14-U7-16-1T', 'hp-spectre-x360-14', 'Ultra 7 / 16GB / 1TB OLED', 'Xanh đêm', 'Intel Core Ultra 7 155H', 'Intel Arc', 16, 1024, 36900000 FROM dual
    UNION ALL SELECT 'SWIFTGO14-I5-16-512', 'acer-swift-go-14', 'i5 / 16GB / 512GB OLED', 'Bạc', 'Intel Core i5-13500H', 'Intel Iris Xe', 16, 512, 18490000 FROM dual
    UNION ALL SELECT 'SWIFTGO14-I7-16-1T', 'acer-swift-go-14', 'i7 / 16GB / 1TB OLED', 'Bạc', 'Intel Core i7-13700H', 'Intel Iris Xe', 16, 1024, 23490000 FROM dual
    UNION ALL SELECT 'ZENBOOK14-U5-16-512', 'asus-zenbook-14-oled', 'Ultra 5 / 16GB / 512GB', 'Xám không gian', 'Intel Core Ultra 5 125H', 'Intel Arc', 16, 512, 21990000 FROM dual
    UNION ALL SELECT 'ZENBOOK14-U7-32-1T', 'asus-zenbook-14-oled', 'Ultra 7 / 32GB / 1TB', 'Xám không gian', 'Intel Core Ultra 7 155H', 'Intel Arc', 32, 1024, 29990000 FROM dual
    UNION ALL SELECT 'X1C12-U7-32-1T', 'lenovo-thinkpad-x1-carbon-gen12', 'Ultra 7 / 32GB / 1TB', 'Đen', 'Intel Core Ultra 7 165U', 'Intel Graphics', 32, 1024, 44900000 FROM dual
    UNION ALL SELECT 'X1C12-U5-16-512', 'lenovo-thinkpad-x1-carbon-gen12', 'Ultra 5 / 16GB / 512GB', 'Đen', 'Intel Core Ultra 5 135U', 'Intel Graphics', 16, 512, 36900000 FROM dual
    UNION ALL SELECT 'E14G6-U5-16-512', 'lenovo-thinkpad-e14-gen6', 'Ultra 5 / 16GB / 512GB', 'Đen', 'Intel Core Ultra 5 125U', 'Intel Graphics', 16, 512, 20990000 FROM dual
    UNION ALL SELECT 'E14G6-U7-16-1T', 'lenovo-thinkpad-e14-gen6', 'Ultra 7 / 16GB / 1TB', 'Đen', 'Intel Core Ultra 7 155U', 'Intel Graphics', 16, 1024, 25990000 FROM dual
    UNION ALL SELECT 'EB840G11-U5-16-512', 'hp-elitebook-840-g11', 'Ultra 5 / 16GB / 512GB', 'Bạc', 'Intel Core Ultra 5 125U', 'Intel Graphics', 16, 512, 31900000 FROM dual
    UNION ALL SELECT 'LAT5450-U5-16-512', 'dell-latitude-5450', 'Ultra 5 / 16GB / 512GB', 'Xám không gian', 'Intel Core Ultra 5 135U', 'Intel Graphics', 16, 512, 27500000 FROM dual
    UNION ALL SELECT 'PROART16-U9-32-1T', 'asus-proart-studiobook-16', 'Ultra 9 / 32GB / 1TB / RTX 4070', 'Đen', 'Intel Core Ultra 9 185H', 'RTX 4070 8GB', 32, 1024, 62990000 FROM dual
) v JOIN product p ON p.slug = v.slug JOIN color c ON c.name = v.color;

-- Nhân viên (bỏ qua nếu đã tồn tại)
INSERT INTO app_user (username, password_hash, full_name, email, phone, branch_id, auth_provider)
SELECT u.un, u.pw, u.fn, u.em, u.ph, b.id, 'LOCAL'
FROM (
    SELECT 'admin' AS un, '$2b$10$GAJQx/wWX3SnrylQw8Mil.O8p3IdYbY6yLX//hWz6BGJLjI05.4ii' AS pw, 'Quản trị viên' AS fn, 'admin@lapstore.vn' AS em, '0900000001' AS ph, 'Kho tổng Long Biên' AS br FROM dual
    UNION ALL SELECT 'sales01', '$2b$10$/0kjrhfe6oNoJ5e9E22Ofei4ZXzRlrnLKw5/TW2yEM/SiOswAH/0K', 'Nguyễn Thị Lan', 'lan.nguyen@lapstore.vn', '0900000002', 'Lapstore Hà Đông' FROM dual
    UNION ALL SELECT 'sales02', '$2b$10$/0kjrhfe6oNoJ5e9E22Ofei4ZXzRlrnLKw5/TW2yEM/SiOswAH/0K', 'Trần Văn Hùng', 'hung.tran@lapstore.vn', '0900000003', 'Lapstore Cầu Giấy' FROM dual
    UNION ALL SELECT 'consult01', '$2b$10$/0kjrhfe6oNoJ5e9E22Ofei4ZXzRlrnLKw5/TW2yEM/SiOswAH/0K', 'Phạm Thu Trang', 'trang.pham@lapstore.vn', '0900000004', 'Lapstore Hà Đông' FROM dual
    UNION ALL SELECT 'kho01', '$2b$10$/0kjrhfe6oNoJ5e9E22Ofei4ZXzRlrnLKw5/TW2yEM/SiOswAH/0K', 'Lê Văn Nam', 'nam.le@lapstore.vn', '0900000005', 'Kho tổng Long Biên' FROM dual
) u LEFT JOIN branch b ON b.name = u.br
WHERE NOT EXISTS (SELECT 1 FROM app_user x WHERE x.username = u.un);

-- Gán chi nhánh cho nhân viên đã có sẵn
UPDATE app_user u SET u.branch_id = (SELECT b.id FROM branch b WHERE b.name = CASE u.username WHEN 'admin' THEN 'Kho tổng Long Biên' WHEN 'sales01' THEN 'Lapstore Hà Đông' WHEN 'sales02' THEN 'Lapstore Cầu Giấy' WHEN 'consult01' THEN 'Lapstore Hà Đông' WHEN 'kho01' THEN 'Kho tổng Long Biên' END)
WHERE u.username IN ('admin','sales01','sales02','consult01','kho01') AND u.branch_id IS NULL;

-- Gán vai trò (bỏ qua nếu đã có)
INSERT INTO user_role (user_id, role_id, granted_at)
SELECT u.id, r.id, SYSTIMESTAMP
FROM (
    SELECT 'admin' AS un, 'ADMIN' AS rc FROM dual
    UNION ALL SELECT 'sales01', 'SALES' FROM dual
    UNION ALL SELECT 'sales02', 'SALES' FROM dual
    UNION ALL SELECT 'consult01', 'CONSULTANT' FROM dual
    UNION ALL SELECT 'kho01', 'WAREHOUSE_PROMO' FROM dual
) m JOIN app_user u ON u.username = m.un JOIN role r ON r.code = m.rc
WHERE NOT EXISTS (SELECT 1 FROM user_role x WHERE x.user_id = u.id AND x.role_id = r.id);

-- Giá niêm yết đang áp dụng (APPROVED)
INSERT INTO price_history (variant_id, price, effective_from, status, reason, approved_by, created_by)
SELECT v.id, v.current_price, SYSTIMESTAMP - NUMTODSINTERVAL(60, 'DAY'), 'APPROVED', 'Giá niêm yết ban đầu', a.id, a.id
FROM product_variant v JOIN app_user a ON a.username = 'admin';

-- Đề xuất giá chờ duyệt (PENDING)
INSERT INTO price_history (variant_id, price, effective_from, status, reason, created_by)
SELECT v.id, p.price, SYSTIMESTAMP + NUMTODSINTERVAL(p.days_ahead, 'DAY'), 'PENDING', p.reason, k.id
FROM (
    SELECT 'ROG-G16-I7-16-1T' AS sku, 36990000 AS price, 3 AS days_ahead, 'Cạnh tranh giá với đối thủ' AS reason FROM dual
    UNION ALL SELECT 'MBA13-M3-8-256', 26490000, 8, 'Chương trình Back to School' FROM dual
    UNION ALL SELECT 'XPS13-U7-16-512', 31500000, 5, 'Điều chỉnh theo giá nhập mới' FROM dual
) p JOIN product_variant v ON v.sku = p.sku JOIN app_user k ON k.username = 'kho01';

-- Phiếu nhập kho (CONFIRMED)
INSERT INTO goods_receipt (code, supplier_id, branch_id, received_by, received_at, status, note)
SELECT r.code, s.id, b.id, a.id, SYSTIMESTAMP - NUMTODSINTERVAL(20, 'DAY'), 'CONFIRMED', r.note
FROM (
    SELECT 'PN2611-001' AS code, 'FPT Trading' AS sup, 'Lapstore Hà Đông' AS br, 'Nhập lô đầu tháng 11 cho cửa hàng Hà Đông' AS note FROM dual
    UNION ALL SELECT 'PN2611-002', 'Synnex FPT', 'Lapstore Cầu Giấy', 'Nhập lô đầu tháng 11 cho cửa hàng Cầu Giấy' FROM dual
    UNION ALL SELECT 'PN2611-003', 'Digiworld', 'Kho tổng Long Biên', 'Nhập kho tổng' FROM dual
) r JOIN supplier s ON s.name = r.sup JOIN branch b ON b.name = r.br JOIN app_user a ON a.username = 'admin';

-- Dòng phiếu nhập
INSERT INTO goods_receipt_item (receipt_id, variant_id, quantity, unit_cost)
SELECT r.id, v.id, i.qty, i.cost
FROM (
    SELECT 'PN2611-001' AS rcode, 'ROG-G16-I7-16-512' AS sku, 6 AS qty, 29231000 AS cost FROM dual
    UNION ALL SELECT 'PN2611-001', 'ROG-G16-I7-16-1T', 6, 33531000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'KATANA15-I7-16-512', 5, 23211000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'KATANA15-I5-8-512', 5, 17191000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'NITROV15-I5-16-512', 6, 18911000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'MBA13-M3-8-256', 6, 24071000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'MBA13-M3-16-512', 5, 30951000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'XPS13-U7-16-512', 5, 27950000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'XPS13-U7-32-1T', 4, 35690000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'SPECTRE14-U7-16-1T', 5, 31734000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'SWIFTGO14-I5-16-512', 6, 15901000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'SWIFTGO14-I7-16-1T', 5, 20201000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'ZENBOOK14-U5-16-512', 6, 18911000 FROM dual
    UNION ALL SELECT 'PN2611-002', 'ZENBOOK14-U7-32-1T', 4, 25791000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'X1C12-U7-32-1T', 4, 38614000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'X1C12-U5-16-512', 5, 31734000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'E14G6-U5-16-512', 6, 18051000 FROM dual
    UNION ALL SELECT 'PN2611-003', 'E14G6-U7-16-1T', 6, 22351000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'EB840G11-U5-16-512', 4, 27434000 FROM dual
    UNION ALL SELECT 'PN2611-001', 'LAT5450-U5-16-512', 5, 23650000 FROM dual
    UNION ALL SELECT 'PN2611-003', 'PROART16-U9-32-1T', 3, 54171000 FROM dual
) i JOIN goods_receipt r ON r.code = i.rcode JOIN product_variant v ON v.sku = i.sku;

-- Serial từng máy (IN_STOCK)
INSERT INTO serial_item (variant_id, serial_number, branch_id, status, goods_receipt_item_id, cost_price)
SELECT i.variant_id, v.sku || '-' || LPAD(n.k, 3, '0'), r.branch_id, 'IN_STOCK', i.id, i.unit_cost
FROM goods_receipt_item i
JOIN goods_receipt r ON r.id = i.receipt_id
JOIN product_variant v ON v.id = i.variant_id
JOIN (SELECT LEVEL AS k FROM dual CONNECT BY LEVEL <= 20) n ON n.k <= i.quantity;

-- Khách hàng
INSERT INTO customer (phone, full_name, email, registered_at)
SELECT c.ph, c.fn, c.em, SYSTIMESTAMP - NUMTODSINTERVAL(90, 'DAY')
FROM (
    SELECT '0901234567' AS ph, 'Nguyễn Văn An' AS fn, 'an.nguyen@example.com' AS em FROM dual
    UNION ALL SELECT '0912345678', 'Trần Thị Bình', 'binh.tran@example.com' FROM dual
    UNION ALL SELECT '0933456789', 'Lê Minh Châu', 'chau.le@example.com' FROM dual
    UNION ALL SELECT '0944567890', 'Phạm Quốc Dũng', 'dung.pham@example.com' FROM dual
    UNION ALL SELECT '0955678901', 'Hoàng Thu Hà', 'ha.hoang@example.com' FROM dual
    UNION ALL SELECT '0966789012', 'Vũ Đức Khánh', 'khanh.vu@example.com' FROM dual
    UNION ALL SELECT '0977890123', 'Đặng Mai Linh', 'linh.dang@example.com' FROM dual
    UNION ALL SELECT '0988901234', 'Bùi Gia Huy', 'huy.bui@example.com' FROM dual
) c;

-- Đơn hàng
INSERT INTO customer_order (order_code, channel, status, customer_id, customer_name, customer_phone, customer_email, fulfillment_method, ship_address_line, fulfill_branch_id, handled_by, confirmed_by, confirmed_at, subtotal, discount_total, shipping_fee, total_amount, payment_status, placed_at, completed_at, cancel_reason)
SELECT o.code, o.ch, o.st, c.id, c.full_name, c.phone, c.email, o.fm, NULLIF(o.addr, '-'), b.id, h.id, cf.id,
       CASE WHEN o.conf <> '-' THEN SYSTIMESTAMP - NUMTODSINTERVAL(GREATEST(o.ago - 1, 0), 'HOUR') END,
       o.sub, o.disc, o.ship, o.total, o.pst,
       SYSTIMESTAMP - NUMTODSINTERVAL(o.ago, 'HOUR'),
       CASE WHEN o.st = 'COMPLETED' THEN SYSTIMESTAMP - NUMTODSINTERVAL(o.done, 'HOUR') END,
       NULLIF(o.cancel, '-')
FROM (
    SELECT 'CT2611-0001' AS code, 'COUNTER' AS ch, 'COMPLETED' AS st, '0901234567' AS phone, 'PICKUP' AS fm, 'Lapstore Hà Đông' AS br, 'sales01' AS handler, 'sales01' AS conf, 150 AS ago, 150 AS done, 38990000 AS sub, 0 AS disc, 0 AS ship, 38990000 AS total, 'PAID' AS pst, '-' AS addr, '-' AS cancel FROM dual
    UNION ALL SELECT 'CT2611-0002', 'COUNTER', 'COMPLETED', '0912345678', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 140, 140, 27990000, 0, 0, 27990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0003', 'COUNTER', 'COMPLETED', '0933456789', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 126, 126, 19990000, 0, 0, 19990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0004', 'COUNTER', 'COMPLETED', '0944567890', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 110, 110, 18490000, 0, 0, 18490000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0005', 'COUNTER', 'COMPLETED', '0955678901', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 98, 98, 20990000, 0, 0, 20990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0006', 'COUNTER', 'COMPLETED', '0966789012', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 86, 86, 32500000, 0, 0, 32500000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0007', 'COUNTER', 'COMPLETED', '0977890123', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 72, 72, 43980000, 0, 0, 43980000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0008', 'COUNTER', 'COMPLETED', '0988901234', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 60, 60, 21990000, 0, 0, 21990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0009', 'COUNTER', 'COMPLETED', '0901234567', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 48, 48, 36900000, 0, 0, 36900000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0010', 'COUNTER', 'COMPLETED', '0912345678', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 36, 36, 35990000, 0, 0, 35990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0011', 'COUNTER', 'COMPLETED', '0933456789', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 26, 26, 33990000, 0, 0, 33990000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0012', 'COUNTER', 'COMPLETED', '0944567890', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 14, 14, 36900000, 0, 0, 36900000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0013', 'COUNTER', 'COMPLETED', '0955678901', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 6, 6, 27500000, 0, 0, 27500000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0014', 'COUNTER', 'COMPLETED', '0966789012', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 3, 3, 23490000, 0, 0, 23490000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'CT2611-0015', 'COUNTER', 'DRAFT', '0977890123', 'PICKUP', 'Lapstore Hà Đông', 'sales01', '-', 1, 0, 38990000, 0, 0, 38990000, 'UNPAID', '-', '-' FROM dual
    UNION ALL SELECT 'LS2611-0001', 'ONLINE', 'NEW', '0901234567', 'DELIVERY', 'Lapstore Hà Đông', '-', '-', 2, 0, 38990000, 0, 0, 38990000, 'UNPAID', '12 Nguyễn Trãi, Hà Đông, Hà Nội', '-' FROM dual
    UNION ALL SELECT 'LS2611-0002', 'ONLINE', 'CONFIRMED', '0933456789', 'PICKUP', 'Lapstore Cầu Giấy', 'sales02', 'sales02', 20, 0, 32500000, 0, 0, 32500000, 'PARTIAL', '-', '-' FROM dual
    UNION ALL SELECT 'LS2611-0003', 'ONLINE', 'COMPLETED', '0955678901', 'PICKUP', 'Lapstore Hà Đông', 'sales01', 'sales01', 100, 96, 20990000, 2099000, 0, 18891000, 'PAID', '-', '-' FROM dual
    UNION ALL SELECT 'LS2611-0004', 'ONLINE', 'CANCELLED', '0944567890', 'DELIVERY', 'Lapstore Cầu Giấy', 'sales02', '-', 70, 0, 23490000, 0, 30000, 23520000, 'UNPAID', '25 Láng Hạ, Đống Đa, Hà Nội', 'Khách đổi ý, đặt lại cấu hình khác' FROM dual
    UNION ALL SELECT 'LS2611-0005', 'ONLINE', 'SHIPPING', '0966789012', 'DELIVERY', 'Lapstore Hà Đông', 'sales01', 'sales01', 30, 0, 26990000, 0, 30000, 27020000, 'UNPAID', '5 Phạm Hùng, Nam Từ Liêm, Hà Nội', '-' FROM dual
    UNION ALL SELECT 'LS2611-0006', 'ONLINE', 'COMPLETED', '0977890123', 'DELIVERY', 'Lapstore Hà Đông', 'sales01', 'sales01', 120, 116, 33990000, 3000000, 30000, 31020000, 'PAID', '99 Trần Duy Hưng, Cầu Giấy, Hà Nội', '-' FROM dual
    UNION ALL SELECT 'LS2611-0007', 'ONLINE', 'NEW', '0988901234', 'DELIVERY', 'Lapstore Cầu Giấy', '-', '-', 1, 0, 35990000, 0, 30000, 36020000, 'UNPAID', '7 Hoàng Quốc Việt, Cầu Giấy, Hà Nội', '-' FROM dual
) o
JOIN customer c ON c.phone = o.phone
JOIN branch b ON b.name = o.br
LEFT JOIN app_user h ON h.username = NULLIF(o.handler, '-')
LEFT JOIN app_user cf ON cf.username = NULLIF(o.conf, '-');

-- Dòng đơn hàng
INSERT INTO order_item (order_id, variant_id, quantity, unit_price, price_history_id, priced_at, line_discount, line_total)
SELECT o.id, v.id, x.qty, v.current_price, ph.id, o.placed_at, x.disc, v.current_price * x.qty - x.disc
FROM (
    SELECT 'CT2611-0001' AS code, 'ROG-G16-I7-16-1T' AS sku, 1 AS qty, 0 AS disc FROM dual
    UNION ALL SELECT 'CT2611-0002', 'MBA13-M3-8-256', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0003', 'KATANA15-I5-8-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0004', 'SWIFTGO14-I5-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0005', 'E14G6-U5-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0006', 'XPS13-U7-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0007', 'NITROV15-I5-16-512', 2, 0 FROM dual
    UNION ALL SELECT 'CT2611-0008', 'ZENBOOK14-U5-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0009', 'X1C12-U5-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0010', 'MBA13-M3-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0011', 'ROG-G16-I7-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0012', 'SPECTRE14-U7-16-1T', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0013', 'LAT5450-U5-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0014', 'SWIFTGO14-I7-16-1T', 1, 0 FROM dual
    UNION ALL SELECT 'CT2611-0015', 'ROG-G16-I7-16-1T', 1, 0 FROM dual
    UNION ALL SELECT 'LS2611-0001', 'ROG-G16-I7-16-1T', 1, 0 FROM dual
    UNION ALL SELECT 'LS2611-0002', 'XPS13-U7-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'LS2611-0003', 'E14G6-U5-16-512', 1, 2099000 FROM dual
    UNION ALL SELECT 'LS2611-0004', 'SWIFTGO14-I7-16-1T', 1, 0 FROM dual
    UNION ALL SELECT 'LS2611-0005', 'KATANA15-I7-16-512', 1, 0 FROM dual
    UNION ALL SELECT 'LS2611-0006', 'ROG-G16-I7-16-512', 1, 3000000 FROM dual
    UNION ALL SELECT 'LS2611-0007', 'MBA13-M3-16-512', 1, 0 FROM dual
) x JOIN customer_order o ON o.order_code = x.code
JOIN product_variant v ON v.sku = x.sku
JOIN price_history ph ON ph.variant_id = v.id AND ph.status = 'APPROVED' AND ph.effective_to IS NULL;

-- Đánh dấu serial đã bán (SOLD)
MERGE INTO serial_item s
USING (
  SELECT m.sn, oi.id AS oi_id, o.completed_at AS done_at
  FROM (
    SELECT 'ROG-G16-I7-16-1T-001' AS sn, 'CT2611-0001' AS code FROM dual
    UNION ALL SELECT 'MBA13-M3-8-256-001', 'CT2611-0002' FROM dual
    UNION ALL SELECT 'KATANA15-I5-8-512-001', 'CT2611-0003' FROM dual
    UNION ALL SELECT 'SWIFTGO14-I5-16-512-001', 'CT2611-0004' FROM dual
    UNION ALL SELECT 'E14G6-U5-16-512-001', 'CT2611-0005' FROM dual
    UNION ALL SELECT 'XPS13-U7-16-512-001', 'CT2611-0006' FROM dual
    UNION ALL SELECT 'NITROV15-I5-16-512-001', 'CT2611-0007' FROM dual
    UNION ALL SELECT 'NITROV15-I5-16-512-002', 'CT2611-0007' FROM dual
    UNION ALL SELECT 'ZENBOOK14-U5-16-512-001', 'CT2611-0008' FROM dual
    UNION ALL SELECT 'X1C12-U5-16-512-001', 'CT2611-0009' FROM dual
    UNION ALL SELECT 'MBA13-M3-16-512-001', 'CT2611-0010' FROM dual
    UNION ALL SELECT 'ROG-G16-I7-16-512-001', 'CT2611-0011' FROM dual
    UNION ALL SELECT 'SPECTRE14-U7-16-1T-001', 'CT2611-0012' FROM dual
    UNION ALL SELECT 'LAT5450-U5-16-512-001', 'CT2611-0013' FROM dual
    UNION ALL SELECT 'SWIFTGO14-I7-16-1T-001', 'CT2611-0014' FROM dual
    UNION ALL SELECT 'E14G6-U5-16-512-002', 'LS2611-0003' FROM dual
    UNION ALL SELECT 'ROG-G16-I7-16-512-002', 'LS2611-0006' FROM dual
  ) m JOIN customer_order o ON o.order_code = m.code
  JOIN order_item oi ON oi.order_id = o.id
) x ON (s.serial_number = x.sn)
WHEN MATCHED THEN UPDATE SET s.status = 'SOLD', s.order_item_id = x.oi_id, s.sold_at = x.done_at,
  s.warranty_start = TRUNC(x.done_at), s.warranty_end = ADD_MONTHS(TRUNC(x.done_at), 12);

-- Lịch sử trạng thái serial: nhập kho
INSERT INTO serial_status_history (serial_id, from_status, to_status, to_branch_id, ref_type, ref_id, changed_by, changed_at)
SELECT s.id, NULL, 'IN_STOCK', s.branch_id, 'RECEIPT', i.receipt_id, a.id, SYSTIMESTAMP - NUMTODSINTERVAL(20, 'DAY')
FROM serial_item s JOIN goods_receipt_item i ON i.id = s.goods_receipt_item_id JOIN app_user a ON a.username = 'admin';

-- Lịch sử trạng thái serial: bán
INSERT INTO serial_status_history (serial_id, from_status, to_status, from_branch_id, ref_type, ref_id, changed_by, changed_at)
SELECT s.id, 'IN_STOCK', 'SOLD', s.branch_id, 'ORDER', o.id, o.handled_by, s.sold_at
FROM serial_item s JOIN order_item oi ON oi.id = s.order_item_id JOIN customer_order o ON o.id = oi.order_id
WHERE s.status = 'SOLD';

-- Lịch sử đơn: tạo
INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, changed_at, reason)
SELECT o.id, NULL, CASE WHEN o.channel = 'COUNTER' THEN 'DRAFT' ELSE 'NEW' END, o.handled_by, o.placed_at, NULL
FROM customer_order o;

-- Lịch sử đơn: xác nhận
INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, changed_at, reason)
SELECT o.id, CASE WHEN o.channel = 'COUNTER' THEN 'DRAFT' ELSE 'NEW' END, 'CONFIRMED', o.confirmed_by, o.confirmed_at, NULL
FROM customer_order o WHERE o.confirmed_at IS NOT NULL;

-- Lịch sử đơn: hoàn tất
INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, changed_at, reason)
SELECT o.id, 'CONFIRMED', 'COMPLETED', o.handled_by, o.completed_at, NULL
FROM customer_order o WHERE o.status = 'COMPLETED';

-- Lịch sử đơn: hủy
INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, changed_at, reason)
SELECT o.id, 'NEW', 'CANCELLED', o.handled_by, o.placed_at + NUMTODSINTERVAL(1, 'HOUR'), o.cancel_reason
FROM customer_order o WHERE o.status = 'CANCELLED';

-- Thanh toán đủ cho đơn hoàn tất
INSERT INTO payment (order_id, method, payment_type, amount, tendered_amount, change_amount, status, received_by, paid_at)
SELECT o.id, m.method, 'FULL', o.total_amount, o.total_amount, 0, 'PAID', o.handled_by, o.completed_at
FROM (
    SELECT 'CT2611-0001' AS code, 'CASH' AS method FROM dual
    UNION ALL SELECT 'CT2611-0002', 'CARD_POS' FROM dual
    UNION ALL SELECT 'CT2611-0003', 'BANK_TRANSFER' FROM dual
    UNION ALL SELECT 'CT2611-0004', 'CASH' FROM dual
    UNION ALL SELECT 'CT2611-0005', 'CARD_POS' FROM dual
    UNION ALL SELECT 'CT2611-0006', 'BANK_TRANSFER' FROM dual
    UNION ALL SELECT 'CT2611-0007', 'CASH' FROM dual
    UNION ALL SELECT 'CT2611-0008', 'CARD_POS' FROM dual
    UNION ALL SELECT 'CT2611-0009', 'BANK_TRANSFER' FROM dual
    UNION ALL SELECT 'CT2611-0010', 'CARD_POS' FROM dual
    UNION ALL SELECT 'CT2611-0011', 'CASH' FROM dual
    UNION ALL SELECT 'CT2611-0012', 'BANK_TRANSFER' FROM dual
    UNION ALL SELECT 'CT2611-0013', 'CARD_POS' FROM dual
    UNION ALL SELECT 'CT2611-0014', 'CASH' FROM dual
    UNION ALL SELECT 'LS2611-0003', 'BANK_TRANSFER' FROM dual
    UNION ALL SELECT 'LS2611-0006', 'COD' FROM dual
) m JOIN customer_order o ON o.order_code = m.code;

-- Đặt cọc đơn online đã xác nhận
INSERT INTO payment (order_id, method, payment_type, amount, status, reference_code, received_by, paid_at)
SELECT o.id, 'BANK_TRANSFER', 'DEPOSIT', 5000000, 'PAID', 'CK261105001', o.confirmed_by, o.confirmed_at
FROM customer_order o WHERE o.order_code = 'LS2611-0002';

-- Giao hàng
INSERT INTO delivery (order_id, carrier_code, tracking_code, status, receiver_name, receiver_phone, receiver_address, shipping_fee, shipping_fee_source, cod_amount, expected_delivery_at, shipped_at, delivered_at, handled_by)
SELECT o.id, 'GHN', CASE o.order_code WHEN 'LS2611-0005' THEN 'GHN2611005' ELSE 'GHN2611006' END,
       CASE o.status WHEN 'COMPLETED' THEN 'DELIVERED' ELSE 'SHIPPING' END,
       o.customer_name, o.customer_phone, o.ship_address_line, o.shipping_fee, 'MANUAL', o.total_amount,
       CASE o.status WHEN 'COMPLETED' THEN o.completed_at ELSE SYSTIMESTAMP + NUMTODSINTERVAL(2, 'DAY') END,
       o.placed_at + NUMTODSINTERVAL(2, 'HOUR'),
       CASE o.status WHEN 'COMPLETED' THEN o.completed_at END,
       o.handled_by
FROM customer_order o WHERE o.order_code IN ('LS2611-0005', 'LS2611-0006');

-- Giữ theo số lượng cho đơn online chưa giao máy
INSERT INTO stock_hold (order_id, order_item_id, variant_id, branch_id, hold_type, quantity, status, held_by, held_at, expires_at)
SELECT o.id, oi.id, oi.variant_id, o.fulfill_branch_id, 'QUANTITY', oi.quantity, 'ACTIVE', NULL, SYSTIMESTAMP, SYSTIMESTAMP + NUMTODSINTERVAL(1, 'DAY')
FROM customer_order o JOIN order_item oi ON oi.order_id = o.id
WHERE o.order_code IN ('LS2611-0001', 'LS2611-0002', 'LS2611-0007');

-- Giữ theo serial (đơn quầy đang mở, đơn đang giao)
INSERT INTO stock_hold (order_id, order_item_id, variant_id, branch_id, hold_type, serial_id, quantity, status, held_by, held_at, expires_at)
SELECT o.id, oi.id, s.variant_id, s.branch_id, 'SERIAL', s.id, 1, 'ACTIVE', u.id, SYSTIMESTAMP, SYSTIMESTAMP + NUMTODSINTERVAL(1, 'DAY')
FROM (
    SELECT 'CT2611-0015' AS code, 'ROG-G16-I7-16-1T-002' AS sn, 'sales01' AS un FROM dual
    UNION ALL SELECT 'LS2611-0005', 'KATANA15-I7-16-512-001', 'sales01' FROM dual
) m JOIN customer_order o ON o.order_code = m.code JOIN order_item oi ON oi.order_id = o.id
JOIN serial_item s ON s.serial_number = m.sn JOIN app_user u ON u.username = m.un;

-- Tồn kho theo (variant, chi nhánh)
INSERT INTO inventory_balance (variant_id, branch_id, on_hand, held_qty)
SELECT variant_id, branch_id, COUNT(*), 0 FROM serial_item WHERE status = 'IN_STOCK' GROUP BY variant_id, branch_id;

-- Cập nhật số lượng đang giữ
UPDATE inventory_balance b SET b.held_qty = (SELECT NVL(SUM(h.quantity), 0) FROM stock_hold h WHERE h.variant_id = b.variant_id AND h.branch_id = b.branch_id AND h.status = 'ACTIVE');

-- Campaign Back to School
INSERT INTO campaign (code, name, discount_type, discount_value, max_discount, min_order_value, priority, is_stackable, status, start_at, end_at, approved_by)
VALUES ('BTS2611', 'Back to School', 'PERCENT', 8, 2000000, 10000000, 10, FALSE, 'ACTIVE', SYSTIMESTAMP - NUMTODSINTERVAL(10, 'DAY'), SYSTIMESTAMP + NUMTODSINTERVAL(20, 'DAY'), (SELECT a.id FROM app_user a WHERE a.username = 'admin'));

-- Campaign Black Friday
INSERT INTO campaign (code, name, discount_type, discount_value, max_discount, min_order_value, priority, is_stackable, status, start_at, end_at)
VALUES ('BF2611', 'Black Friday', 'AMOUNT', 1000000, NULL, 15000000, 20, TRUE, 'SCHEDULED', SYSTIMESTAMP + NUMTODSINTERVAL(17, 'DAY'), SYSTIMESTAMP + NUMTODSINTERVAL(20, 'DAY'));

-- Campaign đã kết thúc
INSERT INTO campaign (code, name, discount_type, discount_value, max_discount, min_order_value, priority, is_stackable, status, start_at, end_at, approved_by)
VALUES ('SUMMER26', 'Hè rực rỡ', 'PERCENT', 5, 1500000, 8000000, 5, FALSE, 'ENDED', SYSTIMESTAMP - NUMTODSINTERVAL(120, 'DAY'), SYSTIMESTAMP - NUMTODSINTERVAL(60, 'DAY'), (SELECT a.id FROM app_user a WHERE a.username = 'admin'));

-- Phạm vi campaign BTS2611: mọi sản phẩm
INSERT INTO campaign_scope (owner_type, owner_id, scope_type, is_exclude)
SELECT 'CAMPAIGN', c.id, 'ALL', FALSE FROM campaign c WHERE c.code = 'BTS2611';

-- Phạm vi campaign BF2611: hãng ASUS
INSERT INTO campaign_scope (owner_type, owner_id, scope_type, target_id, is_exclude)
SELECT 'CAMPAIGN', c.id, 'BRAND', b.id, FALSE FROM campaign c, brand b WHERE c.code = 'BF2611' AND b.name = 'ASUS';

-- Phạm vi campaign SUMMER26: danh mục gaming
INSERT INTO campaign_scope (owner_type, owner_id, scope_type, target_id, is_exclude)
SELECT 'CAMPAIGN', c.id, 'CATEGORY', g.id, FALSE FROM campaign c, category g WHERE c.code = 'SUMMER26' AND g.slug = 'laptop-gaming';

-- Voucher LAP10
INSERT INTO voucher (code, name, audience, discount_type, discount_value, max_discount, min_order, total_limit, per_customer_limit, used_count, reserved_count, stackable_with_campaign, start_at, end_at, status)
VALUES ('LAP10', 'Giảm 10% khi mua laptop', 'PUBLIC', 'PERCENT', 10, 3000000, 10000000, 100, 1, 2, 0, FALSE, SYSTIMESTAMP - NUMTODSINTERVAL(30, 'DAY'), SYSTIMESTAMP + NUMTODSINTERVAL(60, 'DAY'), 'ACTIVE');

-- Voucher NEWUSER
INSERT INTO voucher (code, name, audience, discount_type, discount_value, max_discount, min_order, total_limit, per_customer_limit, used_count, reserved_count, stackable_with_campaign, start_at, end_at, status)
VALUES ('NEWUSER', 'Khách mới giảm 500.000đ', 'PUBLIC', 'AMOUNT', 500000, NULL, 8000000, 50, 1, 0, 0, TRUE, SYSTIMESTAMP - NUMTODSINTERVAL(30, 'DAY'), SYSTIMESTAMP + NUMTODSINTERVAL(60, 'DAY'), 'ACTIVE');

-- Voucher đã hết lượt
INSERT INTO voucher (code, name, audience, discount_type, discount_value, max_discount, min_order, total_limit, per_customer_limit, used_count, reserved_count, stackable_with_campaign, start_at, end_at, status)
VALUES ('WELCOME200', 'Chào mừng giảm 200.000đ', 'PUBLIC', 'AMOUNT', 200000, NULL, 5000000, 30, 1, 30, 0, TRUE, SYSTIMESTAMP - NUMTODSINTERVAL(60, 'DAY'), SYSTIMESTAMP - NUMTODSINTERVAL(5, 'DAY'), 'CLOSED');

-- Lượt dùng voucher LAP10
INSERT INTO voucher_redemption (voucher_id, order_id, customer_id, customer_phone, status, discount_amount, reserved_at, used_at)
SELECT v.id, o.id, o.customer_id, o.customer_phone, 'USED', o.discount_total, o.placed_at, o.completed_at
FROM voucher v JOIN customer_order o ON o.order_code IN ('LS2611-0003', 'LS2611-0006') WHERE v.code = 'LAP10';

-- Dòng giảm giá theo voucher
INSERT INTO order_discount_line (order_id, source_type, source_id, name_snapshot, amount, reason)
SELECT o.id, 'VOUCHER', v.id, v.code, o.discount_total, 'Áp dụng voucher LAP10'
FROM voucher v JOIN customer_order o ON o.order_code IN ('LS2611-0003', 'LS2611-0006') WHERE v.code = 'LAP10';

-- Phân ca 10 ngày (3 ngày trước đến 6 ngày sau, nghỉ mỗi 7 ngày)
INSERT INTO shift_assignment (staff_id, shift_template_id, branch_id, work_date, status, assigned_by)
SELECT u.id, t.id, b.id, d.work_date,
       CASE WHEN d.work_date < TRUNC(SYSDATE) THEN 'COMPLETED' ELSE 'SCHEDULED' END, (SELECT a.id FROM app_user a WHERE a.username = 'admin')
FROM (SELECT TRUNC(SYSDATE) + LEVEL - 4 AS work_date, LEVEL AS lv FROM dual CONNECT BY LEVEL <= 10) d
JOIN (
    SELECT 'sales01' AS un, 'Ca sáng' AS shift, 'Lapstore Hà Đông' AS br FROM dual
    UNION ALL SELECT 'sales02', 'Ca chiều', 'Lapstore Cầu Giấy' FROM dual
    UNION ALL SELECT 'consult01', 'Ca sáng', 'Lapstore Hà Đông' FROM dual
    UNION ALL SELECT 'kho01', 'Ca chiều', 'Kho tổng Long Biên' FROM dual
) m ON MOD(d.lv, 7) <> 0
JOIN app_user u ON u.username = m.un
JOIN shift_template t ON t.name = m.shift
JOIN branch b ON b.name = m.br;

-- Phiên chat
INSERT INTO chat_session (session_token, chat_type, guest_name, guest_phone, assigned_staff_id, status, closed_at)
SELECT c.tok, c.ct, c.gn, c.gp, u.id, c.st, CASE WHEN c.st = 'CLOSED' THEN SYSTIMESTAMP - NUMTODSINTERVAL(20, 'HOUR') END
FROM (
    SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9001' AS tok, 'HUMAN' AS ct, 'Khách Hoàng' AS gn, '0905551111' AS gp, 'consult01' AS staff, 'OPEN' AS st FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9002', 'AI', 'Khách Mai', '0905552222', '-', 'WAITING' FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9003', 'HUMAN', 'Khách Tùng', '0905553333', 'consult01', 'CLOSED' FROM dual
) c LEFT JOIN app_user u ON u.username = NULLIF(c.staff, '-');

-- Đánh dấu phiên được chuyển từ AI
UPDATE chat_session SET handed_over_from_ai = TRUE WHERE session_token = '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9003';

-- Tin nhắn chat
INSERT INTO chat_message (session_id, sender_type, sender_id, content, sent_at)
SELECT s.id, m.st, CASE WHEN m.st = 'STAFF' THEN s.assigned_staff_id END, m.txt, SYSTIMESTAMP - NUMTODSINTERVAL(m.mins, 'MINUTE')
FROM (
    SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9001' AS tok, 'GUEST' AS st, 'Chào shop, mình cần laptop học lập trình tầm 20 triệu.' AS txt, 30 AS mins FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9001', 'AI', 'Xin chào! Bạn ưu tiên pin lâu hay hiệu năng cao ạ?', 29 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9001', 'STAFF', 'Mình là Trang bên tư vấn. Acer Swift Go 14 đang rất hợp ngân sách của bạn.', 20 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9002', 'GUEST', 'Cho mình hỏi MacBook Air M3 còn hàng không?', 15 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9002', 'AI', 'MacBook Air M3 hiện còn tại Lapstore Cầu Giấy. Bạn muốn xem cấu hình nào ạ?', 14 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9003', 'GUEST', 'Máy mình mua tuần trước bị nóng khi chơi game.', 1500 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9003', 'STAFF', 'Bạn thử cập nhật BIOS và vệ sinh quạt, nếu vẫn nóng hãy mang máy đến cửa hàng nhé.', 1480 FROM dual
    UNION ALL SELECT '5f0c2d3a-1b7e-4f6a-9c11-2a8e4b7d9003', 'SYSTEM', 'Phiên chat đã được đóng.', 1200 FROM dual
) m JOIN chat_session s ON s.session_token = m.tok;

-- Yêu cầu tư vấn
INSERT INTO consultation_request (customer_name, phone, variant_id, question, status, assigned_to)
SELECT 'Khách Hoàng', '0905551111', v.id, 'Cần laptop lập trình, ngân sách khoảng 20 triệu.', 'ASSIGNED', u.id
FROM product_variant v, app_user u WHERE v.sku = 'SWIFTGO14-I5-16-512' AND u.username = 'consult01'
UNION ALL
SELECT 'Khách Mai', '0905552222', v.id, 'MacBook Air M3 bản 16GB còn hàng không?', 'NEW', NULL FROM product_variant v WHERE v.sku = 'MBA13-M3-16-512';

-- Yêu cầu đổi trả
INSERT INTO return_request (code, order_id, customer_id, request_type, reason, status, requested_at)
SELECT 'RT2611-0001', o.id, o.customer_id, 'RETURN', 'Máy bị lỗi màn hình sau 3 ngày sử dụng', 'REQUESTED', SYSTIMESTAMP - NUMTODSINTERVAL(24, 'HOUR')
FROM customer_order o WHERE o.order_code = 'LS2611-0006';

-- Tổng hợp doanh thu theo ngày và chi nhánh
INSERT INTO daily_sales_summary (report_date, branch_id, total_orders, cancelled_orders, returned_orders, total_units, gross_revenue, discount_total, refund_total, net_revenue, cost, profit, generated_at)
SELECT a.d, a.b, a.n, 0, 0, c.units, a.gross, a.disc, 0, a.net, c.cost, a.net - c.cost, SYSTIMESTAMP
FROM (SELECT TRUNC(o.completed_at) AS d, o.fulfill_branch_id AS b, COUNT(*) AS n, SUM(o.subtotal) AS gross, SUM(o.discount_total) AS disc, SUM(o.total_amount) AS net
      FROM customer_order o WHERE o.status = 'COMPLETED' GROUP BY TRUNC(o.completed_at), o.fulfill_branch_id) a
JOIN (SELECT TRUNC(o.completed_at) AS d, o.fulfill_branch_id AS b, COUNT(s.id) AS units, SUM(s.cost_price) AS cost
      FROM customer_order o JOIN order_item oi ON oi.order_id = o.id JOIN serial_item s ON s.order_item_id = oi.id
      WHERE o.status = 'COMPLETED' GROUP BY TRUNC(o.completed_at), o.fulfill_branch_id) c ON c.d = a.d AND c.b = a.b;

-- Tổng hợp theo variant
INSERT INTO daily_variant_sales (report_date, variant_id, branch_id, units_sold, units_returned, revenue, cost, profit)
SELECT TRUNC(o.completed_at), oi.variant_id, o.fulfill_branch_id, COUNT(s.id), 0, SUM(oi.unit_price), SUM(s.cost_price), SUM(oi.unit_price - s.cost_price)
FROM customer_order o JOIN order_item oi ON oi.order_id = o.id JOIN serial_item s ON s.order_item_id = oi.id
WHERE o.status = 'COMPLETED'
GROUP BY TRUNC(o.completed_at), oi.variant_id, o.fulfill_branch_id;
