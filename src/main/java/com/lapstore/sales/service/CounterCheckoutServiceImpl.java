package com.lapstore.sales.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lapstore.common.lock.KeyedLock;
import com.lapstore.sales.dto.CounterOrderView;

/** Lớp NGOÀI: giữ KeyedLock "serial:..." khi hoàn tất đơn quầy để tránh xung đột với HoldService / InventoryService. */
@Service
class CounterCheckoutServiceImpl implements CounterCheckoutService {
    private static final Duration WAIT = Duration.ofSeconds(5);

    private final KeyedLock locks;
    private final CounterTx tx;

    CounterCheckoutServiceImpl(KeyedLock locks, CounterTx tx) {
        this.locks = locks;
        this.tx = tx;
    }

    @Override
    public Optional<CounterOrderView> current(String username) {
        return tx.current(username);
    }

    @Override
    public CounterOrderView open(String username) {
        return tx.open(username);
    }

    @Override
    public String complete(long orderId, String customerName, String phone, String username) {
        List<String> keys = tx.heldSerials(orderId).stream()
                .map(sn -> "serial:" + sn)
                .toList();
        if (keys.isEmpty()) {
            return tx.complete(orderId, customerName, phone, username);
        }
        return locks.withLocks(keys, WAIT, () -> tx.complete(orderId, customerName, phone, username));
    }
}
