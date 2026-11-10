package com.lapstore.catalog.dto;

import org.springframework.data.domain.Page;

/** Các biến thể (và số sản phẩm) đang dùng một giá trị thuộc tính, để xem trước khi sửa tên hoặc xóa. */
public record SpecUsageView(String kind, String kindLabel, Long optionId, String label, long variantCount,
        long productCount, Page<SpecUsageRow> rows) {
}
