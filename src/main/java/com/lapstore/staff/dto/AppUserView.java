package com.lapstore.staff.dto;

import java.util.List;

/** locked = đang bị khóa tạm do nhập sai mật khẩu nhiều lần. */
public record AppUserView(Long id, String username, String fullName, String email, String phone,
        Long branchId, String branchName, List<String> roles, boolean active, boolean locked) {
}
