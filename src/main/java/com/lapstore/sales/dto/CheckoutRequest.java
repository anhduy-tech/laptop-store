package com.lapstore.sales.dto;

/** receiveMethod: PICKUP | DELIVERY. address chỉ bắt buộc khi DELIVERY. */
public record CheckoutRequest(String fullName, String phone, String email, String receiveMethod,
                              String address, String voucherCode, Long expectedSubtotal) {
    public CheckoutRequest(String fullName, String phone, String email, String receiveMethod,
                           String address, String voucherCode) {
        this(fullName, phone, email, receiveMethod, address, voucherCode, null);
    }
}
