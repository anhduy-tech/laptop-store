package com.lapstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Bật @PreAuthorize ở lớp service (vd hasAuthority('USER_MANAGE')). */
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
}
