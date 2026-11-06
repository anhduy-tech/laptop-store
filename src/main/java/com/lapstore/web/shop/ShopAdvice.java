package com.lapstore.web.shop;

import jakarta.servlet.http.HttpSession;

import com.lapstore.sales.service.CartService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Số lượng hiển thị trên nút Giỏ của layout/shop. Lỗi service không được làm hỏng cả trang. */
@ControllerAdvice(basePackages = "com.lapstore.web.shop")
class ShopAdvice {
    private final CartService carts;

    ShopAdvice(CartService carts) { this.carts = carts; }

    @ModelAttribute("cartCount")
    int cartCount(HttpSession session) {
        try {
            return carts.count(CartSession.token(session));
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
