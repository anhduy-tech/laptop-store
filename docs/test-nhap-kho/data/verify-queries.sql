-- Các câu SQL kiểm tra nhanh kết quả sau khi thử (Oracle). Đổi mã/serial theo thực tế.

-- Tồn kho của một SKU theo chi nhánh
SELECT br.name AS chi_nhanh, b.on_hand, b.held_qty
  FROM inventory_balance b JOIN product_variant v ON v.id = b.variant_id JOIN branch br ON br.id = b.branch_id
 WHERE v.sku = 'SWIFTGO14-I5-16-512' ORDER BY br.name;

-- Serial thử và trạng thái
SELECT s.serial_number, s.status, br.name AS chi_nhanh, v.sku
  FROM serial_item s JOIN branch br ON br.id = s.branch_id JOIN product_variant v ON v.id = s.variant_id
 WHERE s.serial_number LIKE 'TEST-%' ORDER BY s.serial_number;

-- Lịch sử trạng thái của một serial
SELECT h.changed_at, h.from_status, h.to_status, fb.name AS tu_cn, tb.name AS den_cn, h.ref_type
  FROM serial_status_history h JOIN serial_item s ON s.id = h.serial_id
  LEFT JOIN branch fb ON fb.id = h.from_branch_id LEFT JOIN branch tb ON tb.id = h.to_branch_id
 WHERE s.serial_number = 'TEST-SG14-001' ORDER BY h.changed_at;

-- Biến thể mới đang chờ duyệt giá (INACTIVE + đề xuất PENDING)
SELECT v.sku, v.status, v.current_price, ph.status AS gia_status, ph.reason
  FROM product_variant v JOIN price_history ph ON ph.variant_id = v.id
 WHERE ph.reason LIKE 'Giá bán khởi tạo khi nhập hàng%' ORDER BY ph.created_at DESC;

-- Danh mục thuộc tính CPU/GPU/RAM/ổ cứng (kiểm tra không có hai giá trị trùng nhau)
SELECT d.code, o.value, o.display_name FROM attribute_option o JOIN attribute_definition d ON d.id = o.attribute_id
 ORDER BY d.code, o.display_name;

-- Phiếu chuyển kho gần nhất
SELECT t.code, t.status, fb.name AS tu, tb.name AS den,
       (SELECT COUNT(*) FROM stock_transfer_item i WHERE i.transfer_id = t.id) AS so_may
  FROM stock_transfer t JOIN branch fb ON fb.id = t.from_branch_id JOIN branch tb ON tb.id = t.to_branch_id
 ORDER BY t.shipped_at DESC FETCH FIRST 10 ROWS ONLY;

-- Kết quả một phiên kiểm kê
SELECT i.result, COUNT(*) AS so_dong FROM stocktake_item i JOIN stocktake st ON st.id = i.stocktake_id
 WHERE st.code = 'KK...' GROUP BY i.result;
