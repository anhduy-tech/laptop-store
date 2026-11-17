package com.lapstore.sales.dto;

/** Một khoản giảm hiển thị trên đơn. source: CAMPAIGN | VOUCHER. */
public record DiscountDetail(String source, String name, String rule, long amount) {}
