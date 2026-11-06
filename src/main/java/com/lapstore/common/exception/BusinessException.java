package com.lapstore.common.exception;

/** Lỗi nghiệp vụ cho người dùng thấy (hết hàng, voucher sai, máy đang bị giữ...). Message là tiếng Việt, hiển thị thẳng. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
