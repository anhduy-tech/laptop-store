package com.lapstore.common.exception;

/** Service chưa được cài đặt (đang dùng bản stub). Xóa dần khi các service thật được viết. */
public class ServiceNotReadyException extends BusinessException {
    public ServiceNotReadyException(String what) { super("Chức năng đang được phát triển (" + what + ")."); }
}
