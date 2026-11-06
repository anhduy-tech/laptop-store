package com.lapstore.staff.dto;

import java.util.List;

/** password rỗng khi sửa = giữ mật khẩu cũ. */
public record UserCommand(String username, String fullName, String email, String password,
                          List<String> roles, boolean active) {}
