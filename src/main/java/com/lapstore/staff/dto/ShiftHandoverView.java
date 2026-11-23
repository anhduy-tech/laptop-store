package com.lapstore.staff.dto;

import java.math.BigDecimal;

public record ShiftHandoverView(
        Long id,
        String fromStaff,
        String shift,
        String branch,
        String receiver,
        String createdAt,
        BigDecimal expectedCash,
        BigDecimal handedCash,
        BigDecimal cashVariance,
        BigDecimal receivedCash,
        BigDecimal receiveVariance,
        String note,
        String receiverNote,
        String status) {
}
