package com.lapstore.sales.dto;

import java.util.List;

/**
 * Đơn quầy đang mở. discount = campaignDiscount + voucherDiscount; total = subtotal - discount.
 * secondsLeft: thời gian giữ máy còn lại của máy sắp hết hạn nhất (0 nếu đơn chưa có máy).
 */
public record CounterOrderView(Long id, String code, long subtotal, long campaignDiscount, long voucherDiscount,
                               long discount, long total, long secondsLeft, List<CounterLine> lines,
                               CustomerHit customer, AppliedVoucher voucher, List<DiscountDetail> discounts) {}
