package com.lapstore.catalog.dto;

/** Thông tin được phép sửa. Không có SKU, slug, hãng, màu, giá. */
public record ProductEditRequest(String name, String series, Integer releaseYear, String description,
        String thumbnailUrl, String productStatus, String variantName, String cpu, String gpu, Integer ramGb,
        Integer storageGb, Integer warrantyMonths, Integer lowStockThreshold, Integer returnWindowDays,
        String variantStatus) {
}
