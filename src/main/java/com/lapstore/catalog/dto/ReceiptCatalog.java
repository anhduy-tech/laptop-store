package com.lapstore.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.lapstore.common.dto.IdName;

/** Danh mục cho màn nhập kho: chọn sản phẩm, tạo sản phẩm mới, nhận diện biến thể đã có. */
public record ReceiptCatalog(List<IdName> brands, List<IdName> categories, List<ColorOption> colors,
        List<ProductOption> products, List<ExistingVariant> variants) {

    public record ColorOption(Long id, String name, String hexCode) {
    }

    public record ProductOption(Long id, String name, String brandName) {
    }

    public record ExistingVariant(Long productId, Long id, String sku, Long colorId, String cpu, Integer ramGb,
            Integer storageGb, String status, BigDecimal price) {
    }
}
