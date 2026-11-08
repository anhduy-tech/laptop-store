package com.lapstore.catalog.dto;

import java.math.BigDecimal;

/** Một dòng bảng sản phẩm của nhân viên kho: mỗi dòng là một cấu hình (variant). */
public record ProductAdminRow(Long variantId, Long productId, String productName, String brandName, String sku,
        String variantName, String colorName, BigDecimal price, long onHand, long held, int lowStockThreshold,
        String variantStatus, String productStatus) {

    /** Còn bán được thật = tồn trừ số đang giữ. */
    public long available() {
        return onHand - held;
    }

    public boolean lowStock() {
        return available() <= lowStockThreshold;
    }
}
