package com.lapstore.common.exception;

/** Không lấy được khóa trong thời gian chờ (có người khác đang thao tác cùng đối tượng). */
public class LockBusyException extends BusinessException {
    public LockBusyException(Object key) {
        super("Mục này đang được người khác xử lý, vui lòng thử lại sau giây lát. (" + key + ")");
    }
}
