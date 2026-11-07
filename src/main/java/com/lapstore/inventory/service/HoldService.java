package com.lapstore.inventory.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.dto.HoldResult;

/**
 * Lớp NGOÀI: chỉ lấy khóa rồi gọi HoldTx. Cố ý KHÔNG có @Transactional —
 * nếu có, khóa sẽ được nhả TRƯỚC khi transaction commit và hai quầy vẫn giữ
 * trùng được một máy.
 */
@Service
class HoldService implements StockHoldService {
    private static final Duration WAIT = Duration.ofSeconds(3);

    private final KeyedLock locks;
    private final HoldTx tx;

    HoldService(KeyedLock locks, HoldTx tx) {
        this.locks = locks;
        this.tx = tx;
    }

    @Override
    public HoldResult hold(String serial, long orderId, String username) {
        String sn = normalize(serial);
        return locks.withLock(key(sn), WAIT, () -> tx.hold(sn, orderId, username));
    }

    @Override
    public void release(String serial, long orderId, String username) {
        String sn = normalize(serial);
        locks.withLock(key(sn), WAIT, () -> tx.release(sn, orderId));
    }

    @Override
    public boolean renew(String serial, long orderId, String username) {
        String sn = normalize(serial);
        return locks.withLock(key(sn), WAIT, () -> tx.renew(sn, orderId));
    }

    private static String key(String serialNumber) {
        return "serial:" + serialNumber;
    }

    private static String normalize(String serial) {
        if (serial == null || serial.isBlank())
            throw new BusinessException("Vui lòng nhập serial.");
        return serial.trim();
    }
}
