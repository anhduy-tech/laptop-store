package com.lapstore.staff.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.repo.BranchRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.Role;
import com.lapstore.staff.domain.ShiftAssignment;
import com.lapstore.staff.domain.ShiftTemplate;
import com.lapstore.staff.domain.UserRole;
import com.lapstore.staff.dto.AppUserView;
import com.lapstore.staff.dto.ShiftAssignCommand;
import com.lapstore.staff.dto.ShiftRow;
import com.lapstore.staff.dto.UserCommand;
import com.lapstore.staff.repo.AppUserRepository;
import com.lapstore.staff.repo.RoleRepository;
import com.lapstore.staff.repo.ShiftAssignmentRepository;
import com.lapstore.staff.repo.ShiftTemplateRepository;
import com.lapstore.staff.repo.UserRoleRepository;

/** Tạo / sửa nhân viên, gán vai trò, khóa-mở khóa, xếp ca (AD-01, AD-02). */
@Service
class StaffServiceImpl implements StaffService {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,50}$");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ADMIN_ROLE = "ADMIN";

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final BranchRepository branches;
    private final ShiftTemplateRepository templates;
    private final ShiftAssignmentRepository assignments;
    private final PasswordEncoder encoder;

    StaffServiceImpl(AppUserRepository users, RoleRepository roles, UserRoleRepository userRoles,
            BranchRepository branches, ShiftTemplateRepository templates,
            ShiftAssignmentRepository assignments, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.branches = branches;
        this.templates = templates;
        this.assignments = assignments;
        this.encoder = encoder;
    }

    // ===================== Nhân viên =====================

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<AppUserView> users() {
        List<AppUser> list = users.findByDeletedAtIsNullOrderByUsernameAsc();
        Map<Long, List<String>> roleMap = roleCodesByUser(list.stream().map(AppUser::getId).toList());
        return list.stream().map(u -> view(u, roleMap.getOrDefault(u.getId(), List.of()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public AppUserView user(long id) {
        AppUser u = load(id);
        return view(u, roleCodesByUser(List.of(id)).getOrDefault(id, List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<String> roleCodes() {
        return roles.findAll(Sort.by("code")).stream().map(Role::getCode).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<IdName> branchOptions() {
        return branches.findByActiveTrueOrderByNameAsc().stream().map(b -> new IdName(b.getId(), b.getName())).toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void createUser(UserCommand cmd) {
        String username = normalizeUsername(cmd.username());
        if (users.existsByUsername(username)) {
            throw new BusinessException("Tên đăng nhập “" + username + "” đã tồn tại.");
        }
        String email = blankToNull(cmd.email());
        if (email != null && users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email " + email + " đã được dùng cho tài khoản khác.");
        }
        validatePassword(cmd.password());
        List<Role> wanted = resolveRoles(cmd.roles());

        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(requireText(cmd.fullName(), "Vui lòng nhập họ tên."));
        u.setEmail(email);
        u.setPhone(blankToNull(cmd.phone()));
        u.setBranch(resolveBranch(cmd.branchId()));
        u.setActive(cmd.active());
        u.setPasswordHash(encoder.encode(cmd.password()));
        users.save(u);
        grant(u, wanted);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void updateUser(long id, UserCommand cmd) {
        AppUser u = load(id);
        String email = blankToNull(cmd.email());
        if (email != null && users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Email " + email + " đã được dùng cho tài khoản khác.");
        }
        List<Role> wanted = resolveRoles(cmd.roles());

        List<UserRole> current = userRoles.findAllWithRole(List.of(id));
        boolean isAdminNow = current.stream().anyMatch(ur -> ADMIN_ROLE.equals(ur.getRole().getCode()));
        boolean isAdminAfter = wanted.stream().anyMatch(r -> ADMIN_ROLE.equals(r.getCode()));
        boolean activeAdminNow = isAdminNow && u.isActive();
        boolean activeAdminAfter = isAdminAfter && cmd.active();
        if (activeAdminNow && !activeAdminAfter && userRoles.countActiveAdmins() <= 1) {
            throw new BusinessException("Phải còn ít nhất một quản trị viên đang hoạt động.");
        }
        if (!cmd.active() && u.getUsername().equals(currentUsername())) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản đang đăng nhập.");
        }

        u.setFullName(requireText(cmd.fullName(), "Vui lòng nhập họ tên."));
        u.setEmail(email);
        u.setPhone(blankToNull(cmd.phone()));
        u.setBranch(resolveBranch(cmd.branchId()));
        u.setActive(cmd.active());
        if (blankToNull(cmd.password()) != null) {
            validatePassword(cmd.password());
            u.setPasswordHash(encoder.encode(cmd.password()));
            u.setFailedLoginCount(0);
            u.setLockedUntil(null);
        }
        syncRoles(u, current, wanted);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public void unlockUser(long id) {
        AppUser u = load(id);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
    }

    // ===================== Xếp ca =====================

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SHIFT_MANAGE')")
    public List<IdName> userOptions() {
        return users.findByDeletedAtIsNullOrderByUsernameAsc().stream()
                .filter(AppUser::isActive)
                .map(u -> new IdName(u.getId(), u.getFullName() + " (" + u.getUsername() + ")"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SHIFT_MANAGE')")
    public List<IdName> shiftTemplates() {
        return templates.findByActiveTrueOrderByIdAsc().stream()
                .map(t -> new IdName(t.getId(), t.getName() + " (" + t.getStartTime() + "–" + t.getEndTime() + ")"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SHIFT_MANAGE')")
    public List<ShiftRow> shifts() {
        LocalDate today = LocalDate.now();
        return assignments.findByWorkDateBetweenOrderByWorkDateAscIdAsc(today.minusDays(3), today.plusDays(14)).stream()
                .map(a -> new ShiftRow(a.getWorkDate().format(DMY), a.getStaff().getFullName(),
                        a.getShiftTemplate().getName(), a.getBranch().getName()))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SHIFT_MANAGE')")
    public void assignShift(ShiftAssignCommand cmd) {
        AppUser staff = users.findByIdAndDeletedAtIsNull(cmd.userId())
                .orElseThrow(() -> new BusinessException("Nhân viên không tồn tại."));
        if (!staff.isActive())
            throw new BusinessException("Tài khoản " + staff.getUsername() + " đang bị khóa.");
        ShiftTemplate t = templates.findById(cmd.templateId())
                .filter(ShiftTemplate::isActive)
                .orElseThrow(() -> new BusinessException("Mẫu ca không tồn tại hoặc đã ngừng dùng."));
        Branch branch = resolveBranch(cmd.branchId());
        if (branch == null)
            throw new BusinessException("Vui lòng chọn chi nhánh.");

        for (ShiftAssignment other : assignments.findByStaff_IdAndWorkDateAndStatusNot(staff.getId(), cmd.workDate(),
                "CANCELLED")) {
            ShiftTemplate o = other.getShiftTemplate();
            if (o.getId().equals(t.getId())) {
                throw new BusinessException(staff.getFullName() + " đã được phân ca " + t.getName() + " ngày này.");
            }
            // giờ dạng "HH:mm" nên so sánh chuỗi là đúng thứ tự thời gian
            if (t.getStartTime().compareTo(o.getEndTime()) < 0 && o.getStartTime().compareTo(t.getEndTime()) < 0) {
                throw new BusinessException(
                        "Ca " + t.getName() + " trùng giờ với ca " + o.getName() + " của " + staff.getFullName() + ".");
            }
        }
        ShiftAssignment a = new ShiftAssignment();
        a.setStaff(staff);
        a.setShiftTemplate(t);
        a.setBranch(branch);
        a.setWorkDate(cmd.workDate());
        a.setStatus("SCHEDULED");
        a.setAssignedBy(currentUser());
        assignments.save(a);
    }

    // ===================== Hỗ trợ =====================

    private AppUser load(long id) {
        return users.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên " + id));
    }

    private Map<Long, List<String>> roleCodesByUser(Collection<Long> ids) {
        Map<Long, List<String>> map = new LinkedHashMap<>();
        if (ids.isEmpty())
            return map;
        for (UserRole ur : userRoles.findAllWithRole(ids)) {
            map.computeIfAbsent(ur.getUser().getId(), k -> new ArrayList<>()).add(ur.getRole().getCode());
        }
        map.values().forEach(l -> l.sort(String::compareTo));
        return map;
    }

    private static AppUserView view(AppUser u, List<String> roleCodes) {
        Branch b = u.getBranch();
        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now());
        return new AppUserView(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                b == null ? null : b.getId(), b == null ? null : b.getName(), roleCodes, u.isActive(), locked);
    }

    private List<Role> resolveRoles(List<String> codes) {
        if (codes == null || codes.isEmpty())
            throw new BusinessException("Chọn ít nhất một vai trò.");
        List<Role> result = new ArrayList<>();
        for (String code : new HashSet<>(codes)) {
            result.add(
                    roles.findByCode(code).orElseThrow(() -> new BusinessException("Vai trò không hợp lệ: " + code)));
        }
        return result;
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null)
            return null;
        return branches.findById(branchId).filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException("Chi nhánh không tồn tại hoặc đã ngừng hoạt động."));
    }

    private void syncRoles(AppUser u, List<UserRole> current, List<Role> wanted) {
        Set<String> wantedCodes = new HashSet<>();
        wanted.forEach(r -> wantedCodes.add(r.getCode()));
        Set<String> haveCodes = new HashSet<>();
        List<UserRole> remove = new ArrayList<>();
        for (UserRole ur : current) {
            haveCodes.add(ur.getRole().getCode());
            if (!wantedCodes.contains(ur.getRole().getCode()))
                remove.add(ur);
        }
        userRoles.deleteAll(remove);
        grant(u, wanted.stream().filter(r -> !haveCodes.contains(r.getCode())).toList());
    }

    private void grant(AppUser u, List<Role> toGrant) {
        AppUser by = currentUser();
        for (Role r : toGrant) {
            UserRole ur = new UserRole();
            ur.setUser(u);
            ur.setRole(r);
            ur.setGrantedAt(Instant.now());
            ur.setGrantedBy(by);
            userRoles.save(ur);
        }
    }

    private static String normalizeUsername(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(s).matches()) {
            throw new BusinessException("Tên đăng nhập 3–50 ký tự, chỉ gồm chữ thường, số và dấu . _ -");
        }
        return s;
    }

    /** Tối thiểu 8 ký tự, có cả chữ và số. */
    private static void validatePassword(String p) {
        boolean ok = p != null && p.length() >= 8 && p.length() <= 72
                && p.chars().anyMatch(Character::isLetter) && p.chars().anyMatch(Character::isDigit);
        if (!ok)
            throw new BusinessException("Mật khẩu cần 8–72 ký tự và gồm cả chữ lẫn số.");
    }

    private static String requireText(String s, String message) {
        String t = blankToNull(s);
        if (t == null)
            throw new BusinessException(message);
        return t;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : a.getName();
    }

    private AppUser currentUser() {
        String name = currentUsername();
        return name == null ? null : users.findByUsernameAndDeletedAtIsNull(name).orElse(null);
    }
}
