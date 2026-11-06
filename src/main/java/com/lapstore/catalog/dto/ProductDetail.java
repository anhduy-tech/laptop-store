package com.lapstore.catalog.dto;

import java.util.List;

public record ProductDetail(Long id, String name, String brand, String description,
                            String thumbnail, List<VariantOption> variants) {}
