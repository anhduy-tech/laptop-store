package com.lapstore.aftersales.dto;

/** type: RETURN | EXCHANGE. */
public record ReturnCommand(String orderCode, String phone, String type, String serial, String reason) {}
