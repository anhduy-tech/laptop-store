package com.lapstore.common.exception;

/** Không tìm thấy dữ liệu -> trang 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
