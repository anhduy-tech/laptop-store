package com.lapstore.inventory.service;

import java.util.List;

import com.lapstore.common.dto.IdName;
import com.lapstore.inventory.dto.ReceiptResult;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.dto.StockTransferRequest;

/** Nhập kho, chuyển kho (WH-01, WH-02) và các danh mục cho ô chọn. */
public interface InventoryService {
    List<IdName> suppliers();

    List<IdName> branches();

    /**
     * Nhập kho: tạo sản phẩm/biến thể mới khi cần, sinh serial IN_STOCK, cộng on_hand, ghi lịch sử. Một transaction.
     */
    ReceiptResult createReceipt(StockReceiptRequest request, String username);

    /** Xuất máy khỏi chi nhánh nguồn (serial -> IN_TRANSIT). @return mã phiếu chuyển */
    String transfer(StockTransferRequest request, String username);

    /**
     * Chi nhánh đích nhận hàng: serial IN_TRANSIT -> IN_STOCK tại đích, cộng on_hand đích.
     * Chưa có màn hình cho bước này (xem endpoint POST /staff/warehouse/transfers/{code}/receive).
     */
    void receiveTransfer(String transferCode, String username);
}
