package com.lapstore.staff.service;

import java.util.List;

import com.lapstore.common.dto.IdName;
import com.lapstore.staff.dto.AppUserView;
import com.lapstore.staff.dto.ShiftAssignCommand;
import com.lapstore.staff.dto.ShiftRow;
import com.lapstore.staff.dto.UserCommand;

/**
 * Quản lý nhân viên và xếp ca (AD-01, AD-02). Quyền kiểm bằng @PreAuthorize ở
 * lớp cài đặt.
 */
public interface StaffService {
    List<AppUserView> users();

    /** @throws com.lapstore.common.exception.NotFoundException */
    AppUserView user(long id);

    /** Mã các vai trò có thể gán (ADMIN, SALES, CONSULTANT, WAREHOUSE_PROMO...). */
    List<String> roleCodes();

    /** Danh sách chi nhánh đang hoạt động, dùng cho ô chọn. */
    List<IdName> branchOptions();

    /**
     * @throws com.lapstore.common.exception.BusinessException trùng tên đăng
     *                                                         nhập/email, mật khẩu
     *                                                         yếu, thiếu vai trò...
     */
    void createUser(UserCommand command);

    void updateUser(long id, UserCommand command);

    /** Mở khóa tài khoản bị khóa tạm do nhập sai mật khẩu. */
    void unlockUser(long id);

    List<IdName> userOptions();

    List<IdName> shiftTemplates();

    List<ShiftRow> shifts();

    void assignShift(ShiftAssignCommand command);
}
