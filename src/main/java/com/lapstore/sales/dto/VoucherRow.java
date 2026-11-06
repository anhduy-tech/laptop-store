package com.lapstore.sales.dto;

public record VoucherRow(String code, Integer used, Integer reserved, Integer limit, String status) {}
