package com.lapstore.staff.service;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class LapstoreUser extends User {
    private static final long serialVersionUID = 1L;

    private final Long id;

    public LapstoreUser(Long id, String username, String passwordHash, boolean enabled, boolean accountNonLocked,
                        Collection<? extends GrantedAuthority> authorities) {
        super(username, passwordHash, enabled, true, true, accountNonLocked, authorities);
        this.id = id;
    }

    public Long getId() { return id; }
}
