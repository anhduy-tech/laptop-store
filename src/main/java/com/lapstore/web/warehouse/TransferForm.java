package com.lapstore.web.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Form WH-02. serials: mỗi dòng một serial. */
public class TransferForm implements java.io.Serializable {
    @NotNull(message = "Chọn chi nhánh nguồn")
    private Long fromBranchId;

    @NotNull(message = "Chọn chi nhánh đích")
    private Long toBranchId;

    @NotBlank(message = "Nhập ít nhất một serial")
    private String serials;

    public Long getFromBranchId() { return fromBranchId; }
    public void setFromBranchId(Long fromBranchId) { this.fromBranchId = fromBranchId; }
    public Long getToBranchId() { return toBranchId; }
    public void setToBranchId(Long toBranchId) { this.toBranchId = toBranchId; }
    public String getSerials() { return serials; }
    public void setSerials(String serials) { this.serials = serials; }
}
