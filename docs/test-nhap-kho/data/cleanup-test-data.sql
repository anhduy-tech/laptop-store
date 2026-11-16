-- DỌN DỮ LIỆU SAU KHI THỬ (Oracle). Chạy trên DB dev, ĐỌC KỸ trước khi chạy; script viết theo schema V1–V5, chưa chạy thử trên máy bạn.
-- Quy ước khi thử: serial của dữ liệu thử bắt đầu bằng TEST-, ghi chú phiếu nhập bắt đầu bằng TEST, tên sản phẩm mới bắt đầu bằng "TEST ",
-- ghi chú/phạm vi phiên kiểm kê bắt đầu bằng TEST.
--
-- >>> Sửa mốc thời gian dưới đây thành thời điểm bạn BẮT ĐẦU thử (dùng để nhận ra biến thể/giá trị thuộc tính do lúc thử tạo ra).
DEFINE since = "TIMESTAMP '2026-11-01 00:00:00 +07:00'"

-- 0) Hoàn tác điều chỉnh của kịch bản kiểm kê (F2): trả 2 máy E14G6 về IN_STOCK và máy SWIFTGO14 về Cầu Giấy
UPDATE serial_item SET status = 'IN_STOCK'
 WHERE serial_number IN ('E14G6-U7-16-1T-005', 'E14G6-U7-16-1T-006');
UPDATE serial_item SET branch_id = (SELECT id FROM branch WHERE name = 'Lapstore Cầu Giấy')
 WHERE serial_number = 'SWIFTGO14-I5-16-512-003';
DELETE FROM serial_status_history WHERE ref_type = 'STOCKTAKE';

-- 1) Phiên kiểm kê thử
DELETE FROM stocktake_item WHERE stocktake_id IN (SELECT id FROM stocktake WHERE scope LIKE 'TEST%');
DELETE FROM stocktake WHERE scope LIKE 'TEST%';
DELETE FROM stocktake_item WHERE serial_id IN (SELECT id FROM serial_item WHERE serial_number LIKE 'TEST-%');

-- 2) Phiếu chuyển có máy thử (xóa dòng rồi xóa phiếu không còn dòng nào)
DELETE FROM stock_transfer_item WHERE serial_id IN (SELECT id FROM serial_item WHERE serial_number LIKE 'TEST-%');
DELETE FROM stock_transfer t WHERE NOT EXISTS (SELECT 1 FROM stock_transfer_item i WHERE i.transfer_id = t.id)
   AND t.shipped_at >= &since;

-- 3) Serial thử, lịch sử và phiếu nhập thử
DELETE FROM serial_status_history WHERE serial_id IN (SELECT id FROM serial_item WHERE serial_number LIKE 'TEST-%');
DELETE FROM serial_item WHERE serial_number LIKE 'TEST-%';
DELETE FROM goods_receipt_item WHERE receipt_id IN (SELECT id FROM goods_receipt WHERE note LIKE 'TEST%');
DELETE FROM goods_receipt WHERE note LIKE 'TEST%';

-- 4) Biến thể do lúc thử tạo ra (nhập kho tạo biến thể mới) và giá đề xuất kèm theo
DELETE FROM inventory_balance WHERE variant_id IN (SELECT id FROM product_variant WHERE created_at >= &since);
DELETE FROM price_history WHERE variant_id IN (SELECT id FROM product_variant WHERE created_at >= &since);
DELETE FROM product_variant WHERE created_at >= &since;

-- 5) Sản phẩm thử và hãng do lúc thử tạo ra
DELETE FROM product WHERE name LIKE 'TEST %' AND NOT EXISTS (SELECT 1 FROM product_variant v WHERE v.product_id = product.id);
DELETE FROM brand WHERE name = 'Gigabyte' AND NOT EXISTS (SELECT 1 FROM product p WHERE p.brand_id = brand.id);

-- 6) Giá trị thuộc tính do lúc thử tạo ra (CPU AMD Ryzen 7 8845HS, RAM 24 GB). Bỏ qua dòng nào bạn đã dùng cho dữ liệu thật.
DELETE FROM attribute_option
 WHERE attribute_id IN (SELECT id FROM attribute_definition WHERE code = 'cpu') AND value = 'AMD Ryzen 7 8845HS';
DELETE FROM attribute_option
 WHERE attribute_id IN (SELECT id FROM attribute_definition WHERE code = 'ram') AND value = '24';

-- 7) Tính lại tồn kho từ serial (khớp cách V2 dựng ban đầu); bỏ các dòng tồn bằng 0 mới phát sinh
UPDATE inventory_balance b SET on_hand = (SELECT COUNT(*) FROM serial_item s
 WHERE s.variant_id = b.variant_id AND s.branch_id = b.branch_id AND s.status = 'IN_STOCK');
DELETE FROM inventory_balance WHERE on_hand = 0 AND held_qty = 0;

COMMIT;

-- Kiểm tra nhanh sau khi dọn: không còn serial/sản phẩm thử, Kho tổng còn đúng 9 máy IN_STOCK
SELECT COUNT(*) AS serial_test_con_lai FROM serial_item WHERE serial_number LIKE 'TEST-%';
SELECT COUNT(*) AS may_kho_tong FROM serial_item s JOIN branch b ON b.id = s.branch_id
 WHERE b.name = 'Kho tổng Long Biên' AND s.status = 'IN_STOCK';
