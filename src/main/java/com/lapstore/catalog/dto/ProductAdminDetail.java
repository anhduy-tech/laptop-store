package com.lapstore.catalog.dto;

import java.util.List;

/** Màn chi tiết một sản phẩm cho nhân viên kho: thông tin chung và mọi biến thể kèm tồn kho. */
public record ProductAdminDetail(Long productId, String name, String slug, String brandName, String categoryName,
        String series, Integer releaseYear, String description, String thumbnailUrl, String status,
        List<VariantAdminRow> variants) {

    public long totalOnHand() {
        return variants.stream().mapToLong(VariantAdminRow::onHand).sum();
    }

    public long totalHeld() {
        return variants.stream().mapToLong(VariantAdminRow::held).sum();
    }
}
