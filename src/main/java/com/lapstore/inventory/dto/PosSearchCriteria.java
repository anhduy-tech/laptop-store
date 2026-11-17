package com.lapstore.inventory.dto;

/** Bộ lọc popup chọn sản phẩm. Trường null/trống = không lọc. Giá tính bằng đồng. */
public record PosSearchCriteria(String q, Long brandId, String cpu, String gpu, Integer ramGb, Integer storageGb,
                                Long minPrice, Long maxPrice, boolean onlyAvailable) {}
