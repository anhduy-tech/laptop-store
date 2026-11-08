package com.lapstore.inventory.dto;

import java.math.BigDecimal;

/** Một máy (serial). costPrice chỉ nên hiển thị cho người có quyền COST_VIEW. */
public record StockSerialRow(String serialNumber, String status, BigDecimal costPrice, String receivedAt) {
}
