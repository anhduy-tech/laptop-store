package com.lapstore.sales.dto;

import java.util.List;

public record OrderDetail(Long id, String code, String status, String customerName, String phone,
                          String address, Long total, List<OrderItemDetail> items) {}
