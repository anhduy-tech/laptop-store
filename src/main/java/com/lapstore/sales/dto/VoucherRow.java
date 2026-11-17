package com.lapstore.sales.dto;

public record VoucherRow(Long id, String code, String name, String value, String period, Integer used, Integer reserved,
                         String limit, String status, boolean stackable, boolean canToggle) {}
