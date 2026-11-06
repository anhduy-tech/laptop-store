package com.lapstore.reporting.dto;

/** date đã định dạng dd/MM. */
public record ReportRow(String date, String branch, Integer orders, Long revenue, Long profit) {}
