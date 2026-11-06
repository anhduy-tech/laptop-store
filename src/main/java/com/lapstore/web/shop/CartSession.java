package com.lapstore.web.shop;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

/** Token giỏ hàng của khách vãng lai, giữ trong HttpSession. */
final class CartSession {
    private static final String KEY = "CART_TOKEN";

    private CartSession() {}

    static String token(HttpSession session) {
        Object t = session.getAttribute(KEY);
        if (t instanceof String s) return s;
        String created = UUID.randomUUID().toString();
        session.setAttribute(KEY, created);
        return created;
    }
}
