package com.lapstore.sales.dto;

import java.util.List;

public record CartView(List<CartItemView> items, long total, int count) {
    public static CartView empty() { return new CartView(List.of(), 0, 0); }
}
