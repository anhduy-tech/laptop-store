package com.lapstore.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lapstore.staff.service.LapstoreUser;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class JpaAuditingConfig {

    @Bean
    AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.isAuthenticated() && a.getPrincipal() instanceof LapstoreUser u) {
                return Optional.of(u.getId());
            }
            return Optional.empty();
        };
    }
}
