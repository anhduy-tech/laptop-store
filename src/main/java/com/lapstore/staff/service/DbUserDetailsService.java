package com.lapstore.staff.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.repo.AppUserRepository;
import com.lapstore.staff.repo.PermissionRepository;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;
    private final PermissionRepository permissions;

    public DbUserDetailsService(AppUserRepository users, PermissionRepository permissions) {
        this.users = users;
        this.permissions = permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        List<GrantedAuthority> authorities = permissions.codesOfUser(u.getId()).stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now());
        return new LapstoreUser(u.getId(), u.getUsername(), u.getPasswordHash(), u.isActive(), !locked, authorities);
    }
}
