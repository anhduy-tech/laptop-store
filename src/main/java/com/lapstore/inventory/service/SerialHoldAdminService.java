package com.lapstore.inventory.service;

/**
 * Quản lý giữ máy phía kho: nhả thủ công một serial đang bị giữ (vd. nhân viên bán hàng lỡ đóng/xóa trang
 * khi đang lên đơn quầy) thay vì chờ HoldExpiryJob dọn.
 */
public interface SerialHoldAdminService {
    /**
     * Nhả hold ACTIVE của máy. Đơn quầy nháp vẫn còn, chỉ mất dòng máy này; nhân viên quét/chọn lại.
     *
     * @return kết quả cho thông báo
     * @throws com.lapstore.common.exception.NotFoundException serial không tồn tại
     * @throws com.lapstore.common.exception.BusinessException đơn của hold đã qua bước nháp
     * @throws com.lapstore.common.exception.LockBusyException chờ khóa quá hạn
     */
    Result release(String serialNumber, String username);

    /** @param released false nếu máy không có hold nào; @param orderCode đơn bị nhả (rỗng nếu hold không gắn đơn) */
    record Result(boolean released, String orderCode) {
    }
}
