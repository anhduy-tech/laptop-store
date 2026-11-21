package com.lapstore.sales.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.service.StockHoldService;

/** Lớp NGOÀI: giữ KeyedLock khi thao tác serial, không mở @Transactional ở đây. */
@Service
class OrderServiceImpl implements OrderService {
    private static final Duration WAIT = Duration.ofSeconds(5);

    private final OrderTx tx;
    private final StockHoldService holds;
    private final KeyedLock locks;

    OrderServiceImpl(OrderTx tx, StockHoldService holds, KeyedLock locks) {
        this.tx = tx;
        this.holds = holds;
        this.locks = locks;
    }

    @Override
    public void confirm(long orderId, String username) {
        tx.confirm(orderId, username);
    }

    @Override
    public void complete(long orderId, String username) {
        List<String> keys = tx.heldSerials(orderId).stream()
                .map(sn -> "serial:" + sn)
                .toList();
        if (keys.isEmpty()) {
            tx.complete(orderId, username);
            return;
        }
        locks.withLocks(keys, WAIT, () -> {
            tx.complete(orderId, username);
            return null;
        });
    }

    @Override
    public void cancel(long orderId, String reason, String username) {
        List<String> keys = tx.heldSerials(orderId).stream()
                .map(sn -> "serial:" + sn)
                .toList();
        if (keys.isEmpty()) {
            tx.cancel(orderId, reason, username);
            return;
        }
        locks.withLocks(keys, WAIT, () -> {
            tx.cancel(orderId, reason, username);
            return null;
        });
    }

    @Override
    public void assignSerial(long orderId, long itemId, String serial, String username) {
        if (serial == null || serial.isBlank()) {
            throw new BusinessException("Vui lòng nhập serial.");
        }
        String sn = serial.trim();
        tx.validateBeforeAssign(orderId, itemId, sn);
        holds.hold(sn, orderId, username);
        tx.linkHoldToItem(orderId, itemId, sn);
    }
}
