package com.lapstore.web.warehouse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form WH-05 sửa sản phẩm (sản phẩm + một cấu hình). Không có SKU, giá. */
public class ProductEditForm implements java.io.Serializable {
    @NotBlank(message = "Nhập tên sản phẩm")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    @Size(max = 80, message = "Dòng sản phẩm tối đa 80 ký tự")
    private String series;

    @Min(value = 1990, message = "Năm ra mắt không hợp lệ")
    @Max(value = 2100, message = "Năm ra mắt không hợp lệ")
    private Integer releaseYear;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @Size(max = 255, message = "Đường dẫn ảnh tối đa 255 ký tự")
    private String thumbnailUrl;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái sản phẩm không hợp lệ")
    private String productStatus;

    @NotBlank(message = "Nhập tên cấu hình")
    @Size(max = 255, message = "Tên cấu hình tối đa 255 ký tự")
    private String variantName;

    @NotBlank(message = "Nhập CPU")
    @Size(max = 100, message = "CPU tối đa 100 ký tự")
    private String cpu;

    @Size(max = 100, message = "GPU tối đa 100 ký tự")
    private String gpu;

    @NotNull(message = "Nhập RAM")
    @Min(value = 1, message = "RAM phải lớn hơn 0")
    @Max(value = 2048, message = "RAM quá lớn")
    private Integer ramGb;

    @NotNull(message = "Nhập dung lượng ổ cứng")
    @Min(value = 1, message = "Ổ cứng phải lớn hơn 0")
    private Integer storageGb;

    @NotNull(message = "Nhập số tháng bảo hành")
    @Min(value = 0, message = "Bảo hành không được âm")
    @Max(value = 120, message = "Bảo hành tối đa 120 tháng")
    private Integer warrantyMonths;

    @Min(value = 0, message = "Ngưỡng tồn thấp không được âm")
    private Integer lowStockThreshold;

    @Min(value = 0, message = "Số ngày đổi trả không được âm")
    @Max(value = 365, message = "Số ngày đổi trả tối đa 365")
    private Integer returnWindowDays;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái cấu hình không hợp lệ")
    private String variantStatus;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getProductStatus() { return productStatus; }
    public void setProductStatus(String productStatus) { this.productStatus = productStatus; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }
    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }
    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }
    public Integer getRamGb() { return ramGb; }
    public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }
    public Integer getStorageGb() { return storageGb; }
    public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Integer getReturnWindowDays() { return returnWindowDays; }
    public void setReturnWindowDays(Integer returnWindowDays) { this.returnWindowDays = returnWindowDays; }
    public String getVariantStatus() { return variantStatus; }
    public void setVariantStatus(String variantStatus) { this.variantStatus = variantStatus; }
}
