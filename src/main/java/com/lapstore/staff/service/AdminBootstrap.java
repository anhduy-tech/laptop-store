package com.lapstore.staff.service;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.Role;
import com.lapstore.staff.domain.UserRole;
import com.lapstore.staff.repo.AppUserRepository;
import com.lapstore.staff.repo.RoleRepository;
import com.lapstore.staff.repo.UserRoleRepository;


@Component
class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final String demoPassword;
    private final boolean demoUsers;

    AdminBootstrap(AppUserRepository users, RoleRepository roles, UserRoleRepository userRoles,
                   PasswordEncoder encoder,
                   @Value("${app.bootstrap.admin-password:Admin@123}") String adminPassword,
                   @Value("${app.bootstrap.demo-password:Lap@12345}") String demoPassword,
                   @Value("${app.bootstrap.demo-users:false}") boolean demoUsers) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
        this.demoUsers = demoUsers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        create("admin", "Quản trị viên", adminPassword, "ADMIN");
        if (demoUsers) {
            Map.of("sales01", new String[] {"Nguyễn Thị Lan", "SALES"},
                   "consult01", new String[] {"Trần Văn Minh", "CONSULTANT"},
                   "kho01", new String[] {"Lê Văn Nam", "WAREHOUSE_PROMO"})
               .forEach((username, v) -> create(username, v[0], demoPassword, v[1]));
        }
    }

    private void create(String username, String fullName, String rawPassword, String roleCode) {
        if (users.existsByUsername(username)) return;
        Role role = roles.findByCode(roleCode).orElseThrow(
                () -> new IllegalStateException("Thiếu role " + roleCode + " — V1 đã chạy và có seed vai trò chưa?"));

        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setPasswordHash(encoder.encode(rawPassword));
        users.save(u);

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        ur.setGrantedAt(Instant.now());
        userRoles.save(ur);

        log.warn("Đã tạo tài khoản '{}' (vai trò {}). Hãy đổi mật khẩu mặc định khi triển khai thật.", username, roleCode);
    }
}
