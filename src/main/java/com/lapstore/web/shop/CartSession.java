package com.lapstore.web.shop;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

/** Token giỏ hàng của khách vãng lai, giữ trong HttpSession. */
final class CartSession {
    private static final String KEY = "CART_TOKEN";
    private static final String CHECKOUT_SUBTOTAL = "CHECKOUT_SUBTOTAL";

    private CartSession() {}

    static String token(HttpSession session) {
        Object t = session.getAttribute(KEY);
        if (t instanceof String s) return s;
        String created = UUID.randomUUID().toString();
        session.setAttribute(KEY, created);
        return created;
    }

    static void rememberCheckoutSubtotal(HttpSession session, long subtotal) {
        session.setAttribute(CHECKOUT_SUBTOTAL, subtotal);
    }

    static Long checkoutSubtotal(HttpSession session) {
        Object subtotal = session.getAttribute(CHECKOUT_SUBTOTAL);
        return subtotal instanceof Long value ? value : null;
    }

    static void clearCheckoutSubtotal(HttpSession session) {
        session.removeAttribute(CHECKOUT_SUBTOTAL);
    }
}
