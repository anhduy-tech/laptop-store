package com.lapstore.catalog.dto;

/** Đề xuất giá đang chờ duyệt (AD-03). effectiveFrom đã định dạng dd/MM/yyyy. */
public record PriceProposalView(Long id, String sku, Long newPrice, String effectiveFrom, String reason) {}
