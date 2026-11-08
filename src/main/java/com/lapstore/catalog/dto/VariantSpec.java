package com.lapstore.catalog.dto;

import java.math.BigDecimal;

/** Một tổ hợp thuộc tính cần có: tìm biến thể đã có, chưa có thì tạo (cần sellPrice). */
public record VariantSpec(Long productId, Long colorId, String cpu, String gpu, Integer ramGb, Integer storageGb,
        BigDecimal sellPrice) {
}
