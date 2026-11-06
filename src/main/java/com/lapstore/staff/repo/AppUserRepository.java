package com.lapstore.staff.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameAndDeletedAtIsNull(String username);

    /** Kể cả tài khoản đã xóa mềm (username là UNIQUE trong DB). */
    boolean existsByUsername(String username);
}
