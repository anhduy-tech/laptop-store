package com.lapstore.inventory.service;

import java.util.List;

import com.lapstore.common.dto.IdName;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.dto.StockTransferRequest;

/** Nhập kho, chuyển kho (WH-01, WH-02) và các danh mục cho ô chọn. */
public interface InventoryService {
    List<IdName> suppliers();

    List<IdName> branches();

    /** @return mã phiếu nhập */
    String createReceipt(StockReceiptRequest request, String username);

    /** @return mã phiếu chuyển */
    String transfer(StockTransferRequest request, String username);
}
