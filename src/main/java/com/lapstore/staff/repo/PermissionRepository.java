package com.lapstore.staff.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.staff.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("""
            select distinct p.code
            from RolePermission rp
            join rp.permission p
            where rp.role.id in (select ur.role.id from UserRole ur where ur.user.id = :userId)
            """)
    List<String> codesOfUser(@Param("userId") Long userId);
}
