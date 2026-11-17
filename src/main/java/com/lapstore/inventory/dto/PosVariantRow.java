package com.lapstore.inventory.dto;

import java.math.BigDecimal;

/** Một cấu hình tại một chi nhánh trong popup chọn sản phẩm của màn bán quầy. available = tồn - đang giữ. */
public record PosVariantRow(Long variantId, String productName, String variantName, String sku, String color, String cpu,
                            String gpu, Integer ramGb, Integer storageGb, BigDecimal price, Long branchId,
                            String branchName, Integer available) {}
