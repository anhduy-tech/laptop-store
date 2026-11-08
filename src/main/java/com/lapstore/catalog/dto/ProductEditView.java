package com.lapstore.catalog.dto;

import java.math.BigDecimal;

/** Dữ liệu màn sửa sản phẩm. Các ô chỉ đọc: hãng, SKU, màu, giá hiện tại (giá đổi qua đề xuất giá). */
public record ProductEditView(Long variantId, Long productId, String brandName, String sku, String colorName,
        BigDecimal currentPrice,
        // sản phẩm (dùng chung mọi cấu hình)
        String name, String series, Integer releaseYear, String description, String thumbnailUrl,
        String productStatus,
        // cấu hình (variant)
        String variantName, String cpu, String gpu, Integer ramGb, Integer storageGb, Integer warrantyMonths,
        Integer lowStockThreshold, Integer returnWindowDays, String variantStatus) {
}
