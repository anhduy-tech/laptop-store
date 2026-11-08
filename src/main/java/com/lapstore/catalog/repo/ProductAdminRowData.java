package com.lapstore.catalog.repo;

import java.math.BigDecimal;

/** Kết quả truy vấn thô của bảng sản phẩm; tồn kho để kiểu Number vì SUM trả Long/Integer tùy dialect. */
public record ProductAdminRowData(Long variantId, Long productId, String productName, String brandName, String sku,
        String variantName, String colorName, BigDecimal price, Number onHand, Number held, Integer lowStockThreshold,
        String variantStatus, String productStatus) {
}
