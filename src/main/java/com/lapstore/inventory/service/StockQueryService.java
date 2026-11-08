package com.lapstore.inventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lapstore.inventory.dto.StockRow;
import com.lapstore.inventory.dto.StockSerialsView;

/** Xem tồn kho (WH-06). Quyền PORTAL_WAREHOUSE. */
public interface StockQueryService {
    /** @param q khớp tên sản phẩm hoặc SKU; @param branchId null = mọi chi nhánh; @param lowOnly chỉ dòng sắp hết */
    Page<StockRow> search(String q, Long branchId, boolean lowOnly, Pageable pageable);

    /**
     * Serial của một cấu hình tại một chi nhánh (tối đa 500 máy, mới nhập lên đầu).
     *
     * @param status IN_STOCK, IN_TRANSIT, SOLD...; null = tất cả
     * @throws com.lapstore.common.exception.NotFoundException nếu không có cấu hình hoặc chi nhánh
     */
    StockSerialsView serials(long variantId, long branchId, String status);
}
