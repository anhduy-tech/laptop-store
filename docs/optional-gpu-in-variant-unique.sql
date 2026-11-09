-- TÙY CHỌN, KHÔNG nằm trong src/main/resources/db/migration nên Flyway không tự chạy.
--
-- Hiện ràng buộc duy nhất của biến thể là (sản phẩm, màu, CPU, RAM, ổ cứng), chưa có GPU. Hệ quả: hai biến thể chỉ khác
-- mỗi GPU (VD cùng màu/CPU/RAM/ổ cứng, một bản RTX 4050 và một bản RTX 4060) bị DB từ chối; màn nhập kho báo lỗi rõ ràng.
-- Nếu cửa hàng cần bán các biến thể như vậy, chạy file này (hoặc chép thành migration V4__...sql). Không thêm bảng, không đổi
-- dữ liệu: chỉ thay một ràng buộc. Cột gpu NULL (máy không có GPU rời) vẫn được so sánh như một giá trị.
ALTER TABLE product_variant DROP CONSTRAINT uq_product_variant_product_id_color_id_cpu_ram_gb_storage_gb;
ALTER TABLE product_variant ADD CONSTRAINT uq_product_variant_config
  UNIQUE (product_id, color_id, cpu, gpu, ram_gb, storage_gb);
