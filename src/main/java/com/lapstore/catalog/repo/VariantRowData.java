package com.lapstore.catalog.repo;

import java.math.BigDecimal;

/** Kết quả truy vấn thô các biến thể của một sản phẩm; tồn kho để Number vì SUM trả Long/Integer tùy dialect. */
public record VariantRowData(Long variantId, String sku, String variantName, String colorName, String cpu, String gpu,
        Integer ramGb, Integer storageGb, BigDecimal price, Number onHand, Number held, Integer lowStockThreshold,
        String status) {
}
