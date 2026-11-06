package com.lapstore.web.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Form WH-01. action = draft | confirm. */
public class StockReceiptForm implements java.io.Serializable {
    @NotNull(message = "Chọn nhà cung cấp")
    private Long supplierId;

    @NotNull(message = "Chọn chi nhánh")
    private Long branchId;

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    private String note;

    @NotBlank(message = "Nhập dòng hàng (JSON)")
    private String lines;

    private String action = "draft";

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getLines() { return lines; }
    public void setLines(String lines) { this.lines = lines; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
