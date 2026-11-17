package com.lapstore.sales.service;

import java.util.List;

import com.lapstore.sales.dto.CounterOrderSummary;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.dto.CustomerHit;
import com.lapstore.sales.dto.VoucherSuggestion;

/**
 * Bán quầy (SA-01). Mỗi nhân viên có tối đa {@link #MAX_OPEN} đơn quầy đang mở cùng lúc (mỗi đơn là một tab trên màn hình).
 * Mọi thao tác kiểm tra đơn thuộc nhân viên đang đăng nhập.
 */
public interface CounterCheckoutService {
    int MAX_OPEN = 5;

    List<CounterOrderSummary> openOrders(String username);

    /** @throws com.lapstore.common.exception.NotFoundException đơn không có / không còn mở / của người khác */
    CounterOrderView view(long orderId, String username);

    /** @throws com.lapstore.common.exception.BusinessException đã đủ MAX_OPEN đơn */
    CounterOrderView open(String username);

    /** Hủy đơn đang mở: nhả mọi máy đang giữ và lượt voucher, đơn sang CANCELLED. */
    void cancel(long orderId, String reason, String username);

    /** Gắn khách đã có vào đơn; customerId null = bỏ chọn (khách vãng lai). */
    void attachCustomer(long orderId, Long customerId, String username);

    /** "Thêm nhanh": tìm khách theo SĐT, chưa có thì tạo, rồi gắn vào đơn. */
    CustomerHit quickCustomer(long orderId, String name, String phone, String username);

    /** Tìm khách theo tên hoặc một phần SĐT (từ 2 ký tự, tối đa 8 kết quả). */
    List<CustomerHit> searchCustomers(String query);

    /** Giữ một lượt voucher cho đơn (BR-07). Voucher cũ của đơn (nếu có) được nhả. */
    void applyVoucher(long orderId, String code, String username);

    void removeVoucher(long orderId, String username);

    /** Voucher dùng được trước (giảm nhiều nhất = best), rồi voucher còn thiếu điều kiện kèm lý do. */
    List<VoucherSuggestion> voucherSuggestions(long orderId, String username);

    /** Gia hạn giữ máy cho mọi máy trong đơn. @return số máy gia hạn được */
    int renewHolds(long orderId, String username);

    /**
     * Thu tiền, chuyển các serial đang giữ thành SOLD, chốt giá/giảm giá, hoàn tất đơn.
     *
     * @param method    CASH | BANK_TRANSFER | CARD_POS
     * @param tendered  tiền khách đưa (bắt buộc và >= tổng khi CASH; bỏ qua với phương thức khác)
     * @param reference mã giao dịch (chuyển khoản / POS), có thể null
     * @return mã đơn
     */
    String complete(long orderId, String method, Long tendered, String reference, String username);
}
