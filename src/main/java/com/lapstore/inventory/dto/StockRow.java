package com.lapstore.inventory.dto;


/** Một dòng tồn kho: một cấu hình tại một chi nhánh. */
public record StockRow(Long variantId, String productName, String sku, String variantName, String colorName,
        Long branchId, String branchName, int onHand, int held, int lowStockThreshold, String variantStatus) {

    public int available() {
        return onHand - held;
    }

    public boolean lowStock() {
        return available() <= lowStockThreshold;
    }
}
