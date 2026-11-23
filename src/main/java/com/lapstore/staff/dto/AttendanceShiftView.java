package com.lapstore.staff.dto;

import java.util.List;

import com.lapstore.common.dto.IdName;

public record AttendanceShiftView(
        Long assignmentId,
        String date,
        String shiftName,
        String branchName,
        String hours,
        String status,
        String checkIn,
        String checkOut,
        int lateMinutes,
        boolean canCheckIn,
        boolean canCheckOut,
        List<IdName> receivers) {
}
