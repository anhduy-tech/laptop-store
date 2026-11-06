package com.lapstore.sales.service;

import com.lapstore.sales.dto.CheckoutRequest;

/** Chốt đơn online (mục 4.5, 4.7, 4.8): giữ số lượng, tính giá/voucher, tạo đơn, xóa giỏ. */
public interface OnlineOrderService {
    /**
     * @return mã đơn
     * @throws com.lapstore.common.exception.BusinessException giỏ trống, hết hàng, voucher sai...
     */
    String placeOrder(String cartToken, CheckoutRequest request);
}
