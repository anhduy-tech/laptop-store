-- V4: hai biến thể chỉ khác GPU (cùng sản phẩm, màu, CPU, RAM, ổ cứng) là hai biến thể khác nhau.
-- Chỉ thay một ràng buộc duy nhất, không thêm bảng/cột và không đổi dữ liệu.
-- gpu NULL (máy không có GPU rời) vẫn được so sánh như một giá trị: hai biến thể cùng bộ và cùng NULL vẫn bị chặn trùng.
ALTER TABLE product_variant DROP CONSTRAINT uq_product_variant_product_id_color_id_cpu_ram_gb_storage_gb;
ALTER TABLE product_variant ADD CONSTRAINT uq_product_variant_config
  UNIQUE (product_id, color_id, cpu, gpu, ram_gb, storage_gb);
