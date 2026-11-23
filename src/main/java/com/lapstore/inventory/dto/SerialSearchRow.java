package com.lapstore.inventory.dto;

/** Một dòng kết quả tìm serial. status: IN_STOCK | IN_TRANSIT | SOLD | RETURNED | DEFECTIVE | WARRANTY. held: đang có hold ACTIVE. */
public record SerialSearchRow(String serialNumber, String productName, String sku, String variantName,
        String colorName, String branchName, String status, String receivedAt,
        boolean held) {
}
