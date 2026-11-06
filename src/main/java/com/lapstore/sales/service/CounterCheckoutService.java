package com.lapstore.sales.service;

import java.util.Optional;

import com.lapstore.sales.dto.CounterOrderView;

/** Bán quầy (SA-01). Mỗi nhân viên có tối đa một đơn quầy đang mở. */
public interface CounterCheckoutService {
    Optional<CounterOrderView> current(String username);

    CounterOrderView open(String username);

    /** Thu tiền, chuyển các serial đang giữ thành SOLD, hoàn tất đơn. @return mã đơn */
    String complete(long orderId, String customerName, String phone, String username);
}
