package com.lapstore.catalog.repo;

import java.math.BigDecimal;

/**
 * Một dòng kết quả tìm kiếm: sản phẩm + giá thấp nhất trong các cấu hình đang
 * bán.
 */
public record ProductRow(Long id, String name, String brand, String thumbnailUrl, BigDecimal minPrice) {
}
