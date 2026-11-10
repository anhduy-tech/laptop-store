package com.lapstore.inventory.dto;

/** Một máy trong phiếu chuyển. */
public record TransferItemRow(String serialNumber, String productName, String sku, String variantName,
        String serialStatus) {
}
