package com.lapstore.staff.dto;

import java.util.List;

/**
 * password rỗng khi sửa = giữ mật khẩu cũ. username bị bỏ qua khi sửa (không
 * đổi tên đăng nhập).
 */
public record UserCommand(String username, String fullName, String email, String phone, String password,
        Long branchId, List<String> roles, boolean active) {
}
