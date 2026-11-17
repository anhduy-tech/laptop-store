package com.lapstore.web.warehouse;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form tạo campaign (WH-03). */
public class CampaignForm implements java.io.Serializable {
    @NotBlank(message = "Nhập tên campaign")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String name;

    @Pattern(regexp = "PERCENT|AMOUNT|FIXED_PRICE", message = "Loại giảm giá không hợp lệ")
    private String type = "PERCENT";

    @NotNull(message = "Nhập giá trị giảm")
    @Min(value = 1, message = "Giá trị phải lớn hơn 0")
    private Long value;

    @Min(value = 0, message = "Mức giảm tối đa phải >= 0")
    private Long maxDiscount;

    @Min(value = 0, message = "Giá trị đơn tối thiểu phải >= 0")
    private Long minOrder;

    private Integer priority = 0;

    private boolean stackable;

    @NotNull(message = "Chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startAt;

    @NotNull(message = "Chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endAt;

    @Pattern(regexp = "ALL|BRAND|CATEGORY", message = "Phạm vi không hợp lệ")
    private String scopeType = "ALL";

    private Long scopeTargetId;

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
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public boolean isStackable() { return stackable; }
    public void setStackable(boolean stackable) { this.stackable = stackable; }
    public LocalDate getStartAt() { return startAt; }
    public void setStartAt(LocalDate startAt) { this.startAt = startAt; }
    public LocalDate getEndAt() { return endAt; }
    public void setEndAt(LocalDate endAt) { this.endAt = endAt; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getScopeTargetId() { return scopeTargetId; }
    public void setScopeTargetId(Long scopeTargetId) { this.scopeTargetId = scopeTargetId; }
}
