package com.lapstore.sales.dto;

import java.time.LocalDate;

/** type: PERCENT | AMOUNT. audience: PUBLIC (mọi khách). totalLimit null = không giới hạn. */
public record VoucherRequest(String code, String name, String type, Long value, Long maxDiscount, Long minOrder,
                             Integer totalLimit, Integer perCustomerLimit, boolean stackableWithCampaign,
                             LocalDate startAt, LocalDate endAt) {}
