package com.lapstore.catalog.dto;

import java.util.List;

/** Thẻ sản phẩm ở SH-01. price = giá thấp nhất trong các variant đang bán. */
public record ProductCard(Long id, String name, String brand, Long price, Long oldPrice,
                          String sale, List<String> specs, String thumbnail) {}
