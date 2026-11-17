package com.lapstore.sales.dto;

/**
 * Gợi ý voucher cho đơn. eligible=false thì reason cho biết thiếu gì (vd "Cần mua thêm 2.000.000đ để dùng").
 * best: voucher giảm nhiều nhất trong số dùng được. applied: đang áp dụng cho đơn.
 */
public record VoucherSuggestion(String code, String name, String description, long discount, boolean eligible,
                                String reason, boolean best, boolean applied) {}
