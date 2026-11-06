package com.lapstore.staff.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.staff.repo.AppUserRepository;

@Component
class LoginAttemptListener {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private final AppUserRepository users;

    LoginAttemptListener(AppUserRepository users) { this.users = users; }

    @EventListener
    @Transactional
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent e) {
        users.findByUsernameAndDeletedAtIsNull(e.getAuthentication().getName()).ifPresent(u -> {
            int n = (u.getFailedLoginCount() == null ? 0 : u.getFailedLoginCount()) + 1;
            if (n >= MAX_FAILURES) {
                u.setLockedUntil(Instant.now().plus(LOCK_TIME));
                n = 0;
            }
            u.setFailedLoginCount(n);
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent e) {
        users.findByUsernameAndDeletedAtIsNull(e.getAuthentication().getName()).ifPresent(u -> {
            u.setFailedLoginCount(0);
            u.setLockedUntil(null);
            u.setLastLoginAt(Instant.now());
        });
    }
}
