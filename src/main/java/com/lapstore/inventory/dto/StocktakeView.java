package com.lapstore.inventory.dto;

import org.springframework.data.domain.Page;

/**
 * Chi tiết phiên kiểm kê. filter: null (tất cả) | MATCHED | PENDING (chưa quét / thiếu) | UNEXPECTED.
 * adjustable = số dòng thiếu/dư chưa được điều chỉnh.
 */
public record StocktakeView(StocktakeRow header, String filter, Page<StocktakeItemRow> items, long adjustable) {
}
