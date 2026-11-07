package com.lapstore.inventory.service;

import com.lapstore.inventory.dto.HoldResult;

/**
 * Giữ máy theo serial (mục 4.4). Cài đặt: HoldService (KHÔNG @Transactional,
 * giữ KeyedLock "serial:..."),
 * gọi HoldTx (@Transactional) bên trong khóa -> commit xong mới nhả khóa.
 */
public interface StockHoldService {
    /**
     * Giữ máy cho đơn. Quét lại đúng máy đó cho cùng đơn thì trả kết quả, không
     * lỗi.
     * 
     * @throws com.lapstore.common.exception.SerialHeldException         máy đang
     *                                                                   được giữ
     *                                                                   cho đơn
     *                                                                   khác
     * @throws com.lapstore.common.exception.SerialNotAvailableException máy không
     *                                                                   còn
     *                                                                   IN_STOCK
     * @throws com.lapstore.common.exception.LockBusyException           chờ khóa
     *                                                                   quá hạn
     */
    HoldResult hold(String serial, long orderId, String username);

    /** Nhả hold ACTIVE của máy khỏi đơn. Máy không có hold thì bỏ qua. */
    void release(String serial, long orderId, String username);

    /**
     * Heartbeat: gia hạn hold còn hiệu lực. @return false nếu hold đã hết hạn hoặc
     * không thuộc đơn này.
     */
    boolean renew(String serial, long orderId, String username);
}
