package com.lapstore.common.exception;

/** Máy đang được giữ cho đơn khác (hold ACTIVE còn hạn). */
public class SerialHeldException extends BusinessException {
    public SerialHeldException(String serial) {
        super("Máy " + serial + " đang được giữ cho một đơn khác.");
    }
}
