package com.lapstore.staff.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.staff.domain.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("select ur from UserRole ur join fetch ur.role where ur.user.id in :userIds")
    List<UserRole> findAllWithRole(@Param("userIds") Collection<Long> userIds);

    /**
     * Số quản trị viên đang hoạt động — để không cho khóa/gỡ quyền người cuối cùng.
     */
    @Query("""
            select count(distinct ur.user.id) from UserRole ur
            where ur.role.code = 'ADMIN' and ur.user.active = true and ur.user.deletedAt is null
            """)
    long countActiveAdmins();
}
