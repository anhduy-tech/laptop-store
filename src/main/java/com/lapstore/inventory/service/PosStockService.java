package com.lapstore.inventory.service;

import java.util.List;

import com.lapstore.inventory.dto.PosFilterOptions;
import com.lapstore.inventory.dto.PosSearchCriteria;
import com.lapstore.inventory.dto.PosSerialRow;
import com.lapstore.inventory.dto.PosVariantRow;

/** Tra cứu hàng cho popup "Chọn sản phẩm" của màn bán quầy. Quyền PORTAL_SALES. */
public interface PosStockService {
    PosFilterOptions options();

    /** Tối đa 60 dòng (cấu hình × chi nhánh). */
    List<PosVariantRow> search(PosSearchCriteria criteria);

    /** Máy còn bán được của một cấu hình; branchId null = mọi chi nhánh. */
    List<PosSerialRow> serials(long variantId, Long branchId);
}
