package com.lapstore.catalog.dto;

/**
 * Bốn thuộc tính cấu hình máy. Danh mục giá trị nằm ở attribute_definition (code) + attribute_option; giá trị thật của
 * từng biến thể vẫn là cột product_variant.{field}. numeric: giá trị là số GB.
 */
public enum SpecKind {
    CPU("cpu", "CPU", false, "cpu"),
    GPU("gpu", "GPU", false, "gpu"),
    RAM("ram", "RAM", true, "ramGb"),
    STORAGE("storage", "Ổ cứng", true, "storageGb");

    private final String code;
    private final String label;
    private final boolean numeric;
    private final String field;

    SpecKind(String code, String label, boolean numeric, String field) {
        this.code = code;
        this.label = label;
        this.numeric = numeric;
        this.field = field;
    }

    /** attribute_definition.code */
    public String code() { return code; }
    public String label() { return label; }
    public boolean numeric() { return numeric; }
    /** Tên thuộc tính JPA của ProductVariant. */
    public String field() { return field; }
}
