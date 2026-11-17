package com.lapstore.sales.dto;

import java.time.LocalDate;

/**
 * type: PERCENT | AMOUNT | FIXED_PRICE. scopeType: ALL | BRAND | CATEGORY (scopeTargetId bắt buộc khi khác ALL).
 * maxDiscount / minOrder tính bằng đồng, null = không giới hạn / 0.
 */
public record CampaignRequest(String name, String type, Long value, Long maxDiscount, Long minOrder, Integer priority,
                              boolean stackable, LocalDate startAt, LocalDate endAt, String scopeType, Long scopeTargetId) {}
