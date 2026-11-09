package com.lapstore.catalog.dto;

/** Một giá trị thuộc tính kèm số sản phẩm / biến thể đang dùng. */
public record SpecOptionRow(Long id, String value, String label, long productCount, long variantCount,
        boolean renamable) {

    /** Chỉ xóa được khi không còn biến thể nào dùng. */
    public boolean deletable() {
        return variantCount == 0;
    }
}
