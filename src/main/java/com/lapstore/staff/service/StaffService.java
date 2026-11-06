package com.lapstore.staff.service;

import java.util.List;

import com.lapstore.common.dto.IdName;
import com.lapstore.staff.dto.AppUserView;
import com.lapstore.staff.dto.ShiftAssignCommand;
import com.lapstore.staff.dto.ShiftRow;
import com.lapstore.staff.dto.UserCommand;

/** Quản lý nhân viên và xếp ca (AD-01, AD-02). Kiểm quyền ADMIN_ACCESS ở service bằng @PreAuthorize. */
public interface StaffService {
    List<AppUserView> users();

    /** @throws com.lapstore.common.exception.NotFoundException */
    AppUserView user(long id);

    List<String> roleCodes();

    void createUser(UserCommand command);

    void updateUser(long id, UserCommand command);

    /** Dùng cho ô chọn nhân viên ở form xếp ca. */
    List<IdName> userOptions();

    List<IdName> shiftTemplates();

    List<ShiftRow> shifts();

    void assignShift(ShiftAssignCommand command);
}
