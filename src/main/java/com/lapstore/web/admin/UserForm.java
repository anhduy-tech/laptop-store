package com.lapstore.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** Form AD-01 (thêm/sửa nhân viên). password rỗng khi sửa = giữ mật khẩu cũ. */
public class UserForm implements java.io.Serializable {
    @NotBlank(message = "Nhập tên đăng nhập")
    @Size(min = 3, max = 50, message = "Tên đăng nhập 3–50 ký tự")
    private String username;

    @NotBlank(message = "Nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String password;

    private List<String> roles = new ArrayList<>();

    private boolean active = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
