package com.lapstore.sales.dto;

public record OrderRow(Long id, String code, String channel, String customerName, Long total, String status) {}
