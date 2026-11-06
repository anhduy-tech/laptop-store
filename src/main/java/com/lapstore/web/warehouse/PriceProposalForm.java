package com.lapstore.web.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Form WH-04. */
public class PriceProposalForm implements java.io.Serializable {
    @NotNull(message = "Nhập ID variant")
    private Long variantId;

    @NotNull(message = "Nhập giá mới")
    @Positive(message = "Giá phải lớn hơn 0")
    private Long newPrice;

    @NotNull(message = "Chọn ngày hiệu lực")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    @NotBlank(message = "Nhập lý do")
    @Size(max = 300, message = "Lý do tối đa 300 ký tự")
    private String reason;

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Long getNewPrice() { return newPrice; }
    public void setNewPrice(Long newPrice) { this.newPrice = newPrice; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
