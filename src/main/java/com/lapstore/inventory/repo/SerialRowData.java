package com.lapstore.inventory.repo;

import java.time.Instant;

/** Kết quả truy vấn thô tìm serial. */
public record SerialRowData(String serialNumber, String productName, String sku, String variantName, String colorName,
        String branchName, String status, Instant createdAt) {
}
