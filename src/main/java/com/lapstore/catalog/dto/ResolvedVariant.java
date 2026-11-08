package com.lapstore.catalog.dto;

/** created = biến thể vừa được tạo (đang chờ admin duyệt giá bán đầu tiên). */
public record ResolvedVariant(Long variantId, String sku, boolean created) {
}
