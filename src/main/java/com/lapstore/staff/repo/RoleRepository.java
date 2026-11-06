package com.lapstore.staff.repo;

import java.util.Optional;

import com.lapstore.staff.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
}
