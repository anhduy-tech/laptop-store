package com.lapstore.common.exception;

/** Máy không ở trạng thái IN_STOCK (đã bán, đang chuyển kho, bảo hành...). */
public class SerialNotAvailableException extends BusinessException {
    public SerialNotAvailableException(String serial, String status) {
        super("Máy " + serial + " không còn sẵn để bán (trạng thái: " + status + ").");
    }
}
