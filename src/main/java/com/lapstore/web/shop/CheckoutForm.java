package com.lapstore.web.shop;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form SH-04. Giữ dạng JavaBean (getter/setter) để data binding và th: dùng được ổn định. */
public class CheckoutForm implements java.io.Serializable {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email quá dài")
    private String email;

    @Pattern(regexp = "PICKUP|DELIVERY", message = "Hình thức nhận hàng không hợp lệ")
    private String receiveMethod = "PICKUP";

    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String address;

    @Size(max = 30, message = "Mã voucher quá dài")
    private String voucherCode;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getReceiveMethod() { return receiveMethod; }
    public void setReceiveMethod(String receiveMethod) { this.receiveMethod = receiveMethod; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
}
