package com.lapstore.web.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form AD-01 (thêm/sửa nhân viên). password rỗng khi sửa = giữ mật khẩu cũ. */
public class UserForm implements java.io.Serializable {
    @NotBlank(message = "Nhập tên đăng nhập")
    @Size(min = 3, max = 50, message = "Tên đăng nhập 3–50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Tên đăng nhập chỉ gồm chữ, số và dấu . _ -")
    private String username;

    @NotBlank(message = "Nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    private Long branchId;

    private String password;

    private List<String> roles = new ArrayList<>();

    private boolean active = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
