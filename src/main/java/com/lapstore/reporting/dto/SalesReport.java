package com.lapstore.reporting.dto;

import java.util.List;

public record SalesReport(List<ReportRow> rows, int totalOrders, long totalRevenue, long totalProfit) {}
