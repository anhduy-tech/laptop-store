package com.lapstore.inventory.dto;

import java.util.List;
import java.util.Map;

/** Danh sách serial của một cấu hình tại một chi nhánh. countByStatus: số máy theo từng trạng thái. */
public record StockSerialsView(Long variantId, Long branchId, String productName, String sku, String variantName,
        String branchName, Map<String, Long> countByStatus, List<StockSerialRow> serials, boolean truncated) {
}
