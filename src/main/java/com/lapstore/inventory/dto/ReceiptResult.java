package com.lapstore.inventory.dto;

/** Kết quả nhập kho. newVariants = số biến thể vừa tạo, đang chờ admin duyệt giá bán đầu tiên. */
public record ReceiptResult(String code, int serialCount, int newProducts, int newVariants) {
}
