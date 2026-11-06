package com.lapstore.staff.dto;

import java.util.List;

public record AppUserView(Long id, String username, String fullName, String email, List<String> roles, boolean active) {}
