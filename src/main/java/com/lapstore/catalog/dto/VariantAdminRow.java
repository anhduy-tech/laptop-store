package com.lapstore.catalog.dto;

import java.math.BigDecimal;

/** Một biến thể trong màn chi tiết sản phẩm của kho. gpu null = không có. */
public record VariantAdminRow(Long variantId, String sku, String variantName, String colorName, String cpu, String gpu,
        int ramGb, int storageGb, BigDecimal price, long onHand, long held, int lowStockThreshold, String status) {

    public long available() {
        return onHand - held;
    }

    public boolean lowStock() {
        return available() <= lowStockThreshold;
    }
}
