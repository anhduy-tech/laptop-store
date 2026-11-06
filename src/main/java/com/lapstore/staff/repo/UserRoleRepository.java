package com.lapstore.staff.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}
