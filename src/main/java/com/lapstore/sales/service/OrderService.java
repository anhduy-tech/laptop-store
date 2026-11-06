package com.lapstore.sales.service;

/** Thao tác của nhân viên bán hàng lên một đơn (SA-03). */
public interface OrderService {
    void confirm(long orderId, String username);

    void complete(long orderId, String username);

    void cancel(long orderId, String reason, String username);

    /** Gán serial cho dòng hàng; lỗi nếu serial không đúng variant hoặc đang bị giữ/đã bán. */
    void assignSerial(long orderId, long itemId, String serial, String username);
}
