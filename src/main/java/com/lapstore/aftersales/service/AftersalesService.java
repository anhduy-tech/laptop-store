package com.lapstore.aftersales.service;

import com.lapstore.aftersales.dto.ReturnCommand;

public interface AftersalesService {
    /**
     * Tạo yêu cầu đổi trả ở trạng thái PENDING.
     * @throws com.lapstore.common.exception.BusinessException đơn/serial không khớp, quá hạn đổi trả...
     */
    void createReturnRequest(ReturnCommand command);
}
