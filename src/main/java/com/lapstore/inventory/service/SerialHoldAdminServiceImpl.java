package com.lapstore.inventory.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.lapstore.common.lock.KeyedLock;

/**
 * Lớp NGOÀI (không @Transactional): lấy KeyedLock "serial:..." cùng khóa với HoldService rồi mới gọi HoldTx,
 * để không đua với một quầy đang giữ/gia hạn đúng máy đó.
 */
@Service
class SerialHoldAdminServiceImpl implements SerialHoldAdminService {
    private static final Logger log = LoggerFactory.getLogger(SerialHoldAdminServiceImpl.class);
    private static final Duration WAIT = Duration.ofSeconds(3);

    private final KeyedLock locks;
    private final HoldTx tx;

    SerialHoldAdminServiceImpl(KeyedLock locks, HoldTx tx) {
        this.locks = locks;
        this.tx = tx;
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_HOLD_RELEASE')")
    public Result release(String serialNumber, String username) {
        String sn = serialNumber == null ? "" : serialNumber.trim();
        String order = locks.withLock("serial:" + sn, WAIT, () -> tx.forceRelease(sn));
        if (order == null)
            return new Result(false, "");
        log.info("Nhả giữ máy thủ công: serial={}, đơn={}, bởi={}", sn, order.isEmpty() ? "-" : order, username);
        return new Result(true, order);
    }
}
