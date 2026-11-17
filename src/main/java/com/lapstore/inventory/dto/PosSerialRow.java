package com.lapstore.inventory.dto;

/** Máy còn bán được (IN_STOCK, không có hold còn hạn) của một cấu hình. */
public record PosSerialRow(Long id, String serial, Long branchId, String branchName) {}
