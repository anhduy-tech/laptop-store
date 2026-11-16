package com.lapstore.inventory.repo;

import java.time.Instant;

/** Kết quả truy vấn thô các dòng kiểm kê; adjustedById khác null nghĩa là đã điều chỉnh. */
public record StocktakeItemRowData(String serialNumber, String productName, String sku, String variantName,
        Boolean expected, String result, String systemStatus, String systemBranch, Instant scannedAt, String note,
        Long adjustedById) {
}
