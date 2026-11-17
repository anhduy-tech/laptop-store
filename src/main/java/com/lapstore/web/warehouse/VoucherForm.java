package com.lapstore.web.warehouse;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form tạo voucher (WH-03). */
public class VoucherForm implements java.io.Serializable {
    @NotBlank(message = "Nhập mã voucher")
    @Size(max = 30, message = "Mã tối đa 30 ký tự")
    private String code;

    @NotBlank(message = "Nhập tên voucher")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String name;

    @Pattern(regexp = "PERCENT|AMOUNT", message = "Loại giảm giá không hợp lệ")
    private String type = "PERCENT";

    @NotNull(message = "Nhập giá trị giảm")
    @Min(value = 1, message = "Giá trị phải lớn hơn 0")
    private Long value;

    @Min(value = 0, message = "Mức giảm tối đa phải >= 0")
    private Long maxDiscount;

    @Min(value = 0, message = "Giá trị đơn tối thiểu phải >= 0")
    private Long minOrder;

    @Min(value = 1, message = "Giới hạn lượt phải >= 1")
    private Integer totalLimit;

    @Min(value = 1, message = "Giới hạn mỗi khách phải >= 1")
    private Integer perCustomerLimit = 1;

    private boolean stackableWithCampaign;

    @NotNull(message = "Chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startAt;

    @NotNull(message = "Chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endAt;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
    public Long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Long maxDiscount) { this.maxDiscount = maxDiscount; }
    public Long getMinOrder() { return minOrder; }
    public void setMinOrder(Long minOrder) { this.minOrder = minOrder; }
    public Integer getTotalLimit() { return totalLimit; }
    public void setTotalLimit(Integer totalLimit) { this.totalLimit = totalLimit; }
    public Integer getPerCustomerLimit() { return perCustomerLimit; }
    public void setPerCustomerLimit(Integer perCustomerLimit) { this.perCustomerLimit = perCustomerLimit; }
    public boolean isStackableWithCampaign() { return stackableWithCampaign; }
    public void setStackableWithCampaign(boolean stackableWithCampaign) { this.stackableWithCampaign = stackableWithCampaign; }
    public LocalDate getStartAt() { return startAt; }
    public void setStartAt(LocalDate startAt) { this.startAt = startAt; }
    public LocalDate getEndAt() { return endAt; }
    public void setEndAt(LocalDate endAt) { this.endAt = endAt; }
}
