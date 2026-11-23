package com.lapstore.sales.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.service.StockHoldService;

/**
 * Xử lý đơn online của nhân viên (SA-03). Lớp NGOÀI, cố ý KHÔNG @Transactional: lấy KeyedLock "serial:..." (cùng khóa với
 * giữ máy / chuyển kho / kiểm kê / job hết hạn) rồi mới gọi {@link OrderTx}. Hoàn tất và hủy khóa MỌI serial của đơn theo
 * thứ tự chữ cái để hai thao tác song song không khóa chéo.
 */
@Service
class OrderServiceImpl implements OrderService {
    private static final Duration WAIT = Duration.ofSeconds(3);
    private static final String FULFILL = "hasAuthority('ORDER_FULFILL')";
    private static final String CANCEL = "hasAuthority('ORDER_CANCEL')";

    private final KeyedLock locks;
    private final OrderTx tx;
    private final StockHoldService holds;

    OrderServiceImpl(KeyedLock locks, OrderTx tx, StockHoldService holds) {
        this.locks = locks;
        this.tx = tx;
        this.holds = holds;
    }

    @Override
    @PreAuthorize(FULFILL)
    public void confirm(long orderId, String username) {
        tx.confirm(orderId, username);
    }

    @Override
    @PreAuthorize(FULFILL)
    public void assignSerial(long orderId, long itemId, String serial, String username) {
        String sn = serial.trim();
        tx.checkAssign(orderId, itemId, sn);
        holds.hold(sn, orderId, username); // tự khóa serial, đổi 1 đơn vị hold số lượng sang hold serial
    }

    @Override
    @PreAuthorize(FULFILL)
    public void complete(long orderId, String username) {
        lockAll(keys(orderId), 0, () -> tx.complete(orderId, username));
    }

    @Override
    @PreAuthorize(CANCEL)
    public void cancel(long orderId, String reason, String username) {
        lockAll(keys(orderId), 0, () -> {
            tx.cancel(orderId, reason, username);
            return null;
        });
    }

    private List<String> keys(long orderId) {
        List<String> keys = new ArrayList<>();
        for (String sn : tx.activeSerialNumbers(orderId))
            keys.add("serial:" + sn);
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    private <T> T lockAll(List<String> keys, int i, Supplier<T> action) {
        if (i == keys.size())
            return action.get();
        return locks.withLock(keys.get(i), WAIT, () -> lockAll(keys, i + 1, action));
    }
}
