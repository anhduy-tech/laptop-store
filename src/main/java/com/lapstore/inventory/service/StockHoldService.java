package com.lapstore.inventory.service;

import com.lapstore.inventory.dto.HoldResult;

/**
 * Giữ máy theo serial (mục 4.4). Cài đặt gồm HoldService (KHÔNG @Transactional, giữ KeyedLock theo "serial:ID")
 * gọi HoldTx (@Transactional).
 */
public interface StockHoldService {
    /** @throws com.lapstore.common.exception.BusinessException máy không còn hoặc đang được giữ */
    HoldResult hold(String serial, long orderId, String username);

    void release(String serial, long orderId, String username);
}
