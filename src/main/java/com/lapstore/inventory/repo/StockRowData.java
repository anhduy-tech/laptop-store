package com.lapstore.inventory.repo;

/** Kết quả truy vấn thô của bảng tồn kho. */
public record StockRowData(Long variantId, String productName, String sku, String variantName, String colorName,
        Long branchId, String branchName, Integer onHand, Integer held, Integer lowStockThreshold,
        String variantStatus) {
}
