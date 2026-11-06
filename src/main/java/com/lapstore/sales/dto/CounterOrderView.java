package com.lapstore.sales.dto;

import java.util.List;

public record CounterOrderView(Long id, String code, Long total, List<CounterLine> lines) {}
