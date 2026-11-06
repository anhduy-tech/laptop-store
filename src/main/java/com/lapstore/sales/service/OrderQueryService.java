package com.lapstore.sales.service;

import java.util.List;
import java.util.Optional;

import com.lapstore.sales.dto.OrderDetail;
import com.lapstore.sales.dto.OrderRow;
import com.lapstore.sales.dto.OrderView;

/** Truy vấn đơn: khách tra cứu (code + SĐT) và nhân viên xem danh sách/chi tiết. */
public interface OrderQueryService {
    Optional<OrderView> lookup(String code, String phone);

    List<String> statuses();

    List<OrderRow> search(String status, String keyword);

    /** @throws com.lapstore.common.exception.NotFoundException */
    OrderDetail detail(long orderId);

    /** PDF hóa đơn của đơn đã hoàn tất (mục 4.15). */
    byte[] invoicePdf(long orderId);
}
