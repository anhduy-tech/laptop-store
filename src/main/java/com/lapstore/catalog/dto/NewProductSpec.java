package com.lapstore.catalog.dto;

/** Thông tin tạo sản phẩm mới khi nhập hàng. brandId hoặc newBrandName (một trong hai). */
public record NewProductSpec(Long brandId, String newBrandName, Long categoryId, String name, String series,
        Integer releaseYear, String description) {
}
