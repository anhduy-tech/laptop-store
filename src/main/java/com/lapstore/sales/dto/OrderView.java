package com.lapstore.sales.dto;

import java.util.List;

/** Kết quả tra cứu của khách (SH-05). */
public record OrderView(String code, String status, Long total, List<OrderLineView> items) {}
