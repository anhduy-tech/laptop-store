package com.lapstore.sales.service;

import com.lapstore.sales.dto.CartView;

/** Giỏ hàng của khách vãng lai, định danh bằng token lưu trong HttpSession. */
public interface CartService {
    CartView view(String cartToken);

    int count(String cartToken);

    void addItem(String cartToken, long variantId, int quantity);

    void removeItem(String cartToken, long cartItemId);
}
