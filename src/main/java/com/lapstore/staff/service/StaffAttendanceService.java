package com.lapstore.staff.service;

import java.math.BigDecimal;
import java.util.List;

import com.lapstore.staff.domain.ShiftAssignment;
import com.lapstore.staff.dto.AttendanceShiftView;
import com.lapstore.staff.dto.ShiftHandoverView;

public interface StaffAttendanceService {
    List<AttendanceShiftView> today(String username);

    List<ShiftHandoverView> pendingHandovers(String username);

    List<ShiftHandoverView> handoverHistory(String username);

    void checkIn(long assignmentId, BigDecimal openingCash, String username);

    void checkOut(long assignmentId, Long receiverId, BigDecimal countedCash, String note, String username);

    void confirmHandover(long handoverId, BigDecimal receivedCash, String note, String username);

    ShiftAssignment requireActiveAssignment(String username);
}
