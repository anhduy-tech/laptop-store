package com.lapstore.web.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Form WH-03. */
public class CampaignForm implements java.io.Serializable {
    @NotBlank(message = "Nhập tên campaign")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String name;

    @Pattern(regexp = "PERCENT|AMOUNT", message = "Loại giảm giá không hợp lệ")
    private String type = "PERCENT";

    @NotNull(message = "Nhập giá trị giảm")
    @Min(value = 0, message = "Giá trị phải >= 0")
    private Long value;

    @NotNull(message = "Chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startAt;

    @NotNull(message = "Chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
    public LocalDate getStartAt() { return startAt; }
    public void setStartAt(LocalDate startAt) { this.startAt = startAt; }
    public LocalDate getEndAt() { return endAt; }
    public void setEndAt(LocalDate endAt) { this.endAt = endAt; }
}
