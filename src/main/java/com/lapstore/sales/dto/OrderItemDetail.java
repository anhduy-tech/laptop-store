package com.lapstore.sales.dto;

import java.util.List;

public record OrderItemDetail(Long id, String variantName, Long unitPrice, Integer quantity, List<String> serials) {}
