package com.lapstore.chat.dto;

/** mode: HUMAN | AI; status: OPEN | WAITING | CLOSED; lastAt đã định dạng. */
public record ChatSessionRow(Long id, String customerName, String mode, String status, String lastAt) {}
