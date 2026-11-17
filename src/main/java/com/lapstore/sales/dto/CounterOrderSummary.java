package com.lapstore.sales.dto;

/** Một thẻ (tab) đơn đang mở của nhân viên. */
public record CounterOrderSummary(Long id, String code, int lineCount, String customerName) {}
