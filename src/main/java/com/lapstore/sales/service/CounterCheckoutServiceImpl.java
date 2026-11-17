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
import com.lapstore.sales.dto.CounterOrderSummary;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.dto.CustomerHit;
import com.lapstore.sales.dto.VoucherSuggestion;

/**
 * Lớp NGOÀI của bán quầy: KHÔNG @Transactional. Khi chốt đơn lấy KeyedLock "serial:SN" của MỌI máy trong đơn
 * (theo thứ tự chữ cái để hai quầy không khóa chéo nhau), rồi mới gọi CounterTx.complete — commit xong mới nhả khóa.
 * Khóa dùng chung với giữ máy / chuyển kho / kiểm kê / job hết hạn hold. Hủy đơn nhả từng máy qua StockHoldService.
 * Chưa làm: BR-10 (chặn mở đơn khi chưa chấm công / gắn ca); đơn quầy lấy chi nhánh từ các máy đã giữ.
 */
@Service
class CounterCheckoutServiceImpl implements CounterCheckoutService {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final String SALES = "hasAuthority('PORTAL_SALES')";
    private static final String COUNTER = "hasAuthority('ORDER_COUNTER_CREATE')";

    private final CounterTx tx;
    private final KeyedLock locks;
    private final StockHoldService holds;

    CounterCheckoutServiceImpl(CounterTx tx, KeyedLock locks, StockHoldService holds) {
        this.tx = tx;
        this.locks = locks;
        this.holds = holds;
    }

    @Override
    @PreAuthorize(SALES)
    public List<CounterOrderSummary> openOrders(String username) {
        return tx.openOrders(username);
    }

    @Override
    @PreAuthorize(SALES)
    public CounterOrderView view(long orderId, String username) {
        return tx.view(orderId, username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public CounterOrderView open(String username) {
        return tx.open(username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public void cancel(long orderId, String reason, String username) {
        tx.assertOpenOwner(orderId, username);
        for (String sn : tx.activeSerialNumbers(orderId)) holds.release(sn, orderId, username);
        tx.cancel(orderId, reason, username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public void attachCustomer(long orderId, Long customerId, String username) {
        tx.attachCustomer(orderId, customerId, username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public CustomerHit quickCustomer(long orderId, String name, String phone, String username) {
        return tx.quickCustomer(orderId, name, phone, username);
    }

    @Override
    @PreAuthorize(SALES)
    public List<CustomerHit> searchCustomers(String query) {
        return tx.searchCustomers(query);
    }

    @Override
    @PreAuthorize(COUNTER)
    public void applyVoucher(long orderId, String code, String username) {
        tx.applyVoucher(orderId, code, username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public void removeVoucher(long orderId, String username) {
        tx.removeVoucher(orderId, username);
    }

    @Override
    @PreAuthorize(SALES)
    public List<VoucherSuggestion> voucherSuggestions(long orderId, String username) {
        return tx.voucherSuggestions(orderId, username);
    }

    @Override
    @PreAuthorize(COUNTER)
    public int renewHolds(long orderId, String username) {
        tx.assertOpenOwner(orderId, username);
        int n = 0;
        for (String sn : tx.activeSerialNumbers(orderId)) if (holds.renew(sn, orderId, username)) n++;
        return n;
    }

    @Override
    @PreAuthorize(COUNTER)
    public String complete(long orderId, String method, Long tendered, String reference, String username) {
        List<String> keys = new ArrayList<>();
        for (String sn : tx.activeSerialNumbers(orderId)) keys.add("serial:" + sn);
        keys.sort(Comparator.naturalOrder());
        return lockAll(keys, 0, () -> tx.complete(orderId, method, tendered, reference, username));
    }

    private String lockAll(List<String> keys, int i, Supplier<String> action) {
        if (i == keys.size()) return action.get();
        return locks.withLock(keys.get(i), WAIT, () -> lockAll(keys, i + 1, action));
    }
}
