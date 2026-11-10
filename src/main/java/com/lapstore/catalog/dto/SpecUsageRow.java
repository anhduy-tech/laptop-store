package com.lapstore.catalog.dto;

import java.math.BigDecimal;

/** Một biến thể đang dùng một giá trị thuộc tính. */
public record SpecUsageRow(Long productId, String productName, String brandName, Long variantId, String sku,
        String colorName, String cpu, String gpu, Integer ramGb, Integer storageGb, BigDecimal price, String status) {
}
