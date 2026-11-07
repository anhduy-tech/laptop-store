package com.lapstore.staff.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Dùng khi đăng nhập: bỏ qua tài khoản đã xóa mềm. */
    Optional<AppUser> findByUsernameAndDeletedAtIsNull(String username);

    /** Kể cả tài khoản đã xóa mềm (username là UNIQUE trong DB). */
    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "branch")
    Optional<AppUser> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "branch")
    List<AppUser> findByDeletedAtIsNullOrderByUsernameAsc();
}
