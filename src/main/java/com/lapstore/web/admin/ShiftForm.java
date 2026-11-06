package com.lapstore.web.admin;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Form AD-02. */
public class ShiftForm implements java.io.Serializable {
    @NotNull(message = "Chọn nhân viên")
    private Long userId;

    @NotNull(message = "Chọn mẫu ca")
    private Long templateId;

    @NotNull(message = "Chọn chi nhánh")
    private Long branchId;

    @NotNull(message = "Chọn ngày")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
}
