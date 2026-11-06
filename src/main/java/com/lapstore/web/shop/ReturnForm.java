package com.lapstore.web.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form SH-07. */
public class ReturnForm implements java.io.Serializable {
    @NotBlank(message = "Vui lòng nhập mã đơn")
    private String orderCode;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Pattern(regexp = "RETURN|EXCHANGE", message = "Loại yêu cầu không hợp lệ")
    private String type = "RETURN";

    @NotBlank(message = "Vui lòng nhập số serial")
    private String serial;

    @NotBlank(message = "Vui lòng nhập lý do")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
