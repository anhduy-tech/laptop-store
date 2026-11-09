package com.lapstore.web.warehouse;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form WH-01 nhập kho. Trường lồng nhau do receipt.js sinh ra: products[i].variants[j].serials...
 * Dùng class JavaBean (getter/setter) để Spring tự tăng danh sách khi bind chỉ số.
 */
public class StockReceiptForm implements Serializable {
    @NotNull(message = "Chọn nhà cung cấp")
    private Long supplierId;

    @NotNull(message = "Chọn chi nhánh nhận hàng")
    private Long branchId;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;

    private List<ProductBlock> products = new ArrayList<>();

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<ProductBlock> getProducts() { return products; }
    public void setProducts(List<ProductBlock> products) { this.products = products; }

    /** Một sản phẩm: productId có giá trị = sản phẩm đã có, ngược lại là sản phẩm mới (các trường new*). */
    public static class ProductBlock implements Serializable {
        private Long productId;
        private Long brandId;
        private String newBrandName;
        private Long categoryId;
        private String name;
        private String series;
        private Integer releaseYear;
        private String description;
        private List<VariantRow> variants = new ArrayList<>();

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getBrandId() { return brandId; }
        public void setBrandId(Long brandId) { this.brandId = brandId; }
        public String getNewBrandName() { return newBrandName; }
        public void setNewBrandName(String newBrandName) { this.newBrandName = newBrandName; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSeries() { return series; }
        public void setSeries(String series) { this.series = series; }
        public Integer getReleaseYear() { return releaseYear; }
        public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<VariantRow> getVariants() { return variants; }
        public void setVariants(List<VariantRow> variants) { this.variants = variants; }
    }

    /** Một tổ hợp thuộc tính; serials là văn bản, mỗi dòng một serial. */
    public static class VariantRow implements Serializable {
        private Long colorId;
        private String cpu;
        private String gpu;
        private Integer ramGb;
        private Integer storageGb;
        private BigDecimal unitCost;
        private BigDecimal sellPrice;
        private String serials;

        public Long getColorId() { return colorId; }
        public void setColorId(Long colorId) { this.colorId = colorId; }
        public String getCpu() { return cpu; }
        public void setCpu(String cpu) { this.cpu = cpu; }
        public String getGpu() { return gpu; }
        public void setGpu(String gpu) { this.gpu = gpu; }
        public Integer getRamGb() { return ramGb; }
        public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }
        public Integer getStorageGb() { return storageGb; }
        public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
        public BigDecimal getSellPrice() { return sellPrice; }
        public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }
        public String getSerials() { return serials; }
        public void setSerials(String serials) { this.serials = serials; }
    }
}
