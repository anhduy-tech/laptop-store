package com.lapstore.staff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.PaymentRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.Attendance;
import com.lapstore.staff.domain.ShiftAssignment;
import com.lapstore.staff.domain.ShiftHandover;
import com.lapstore.staff.repo.AppUserRepository;
import com.lapstore.staff.repo.AttendanceRepository;
import com.lapstore.staff.repo.ShiftAssignmentRepository;
import com.lapstore.staff.repo.ShiftHandoverRepository;

class StaffAttendanceServiceImplTest {
    private final ShiftAssignmentRepository assignments = org.mockito.Mockito.mock(ShiftAssignmentRepository.class);
    private final AttendanceRepository attendance = org.mockito.Mockito.mock(AttendanceRepository.class);
    private final ShiftHandoverRepository handovers = org.mockito.Mockito.mock(ShiftHandoverRepository.class);
    private final AppUserRepository users = org.mockito.Mockito.mock(AppUserRepository.class);
    private final PaymentRepository payments = org.mockito.Mockito.mock(PaymentRepository.class);
    private final CustomerOrderRepository orders = org.mockito.Mockito.mock(CustomerOrderRepository.class);
    private final StaffAttendanceServiceImpl service = new StaffAttendanceServiceImpl(
            assignments, attendance, handovers, users, payments, orders);

    @Test
    void checkOutReconcilesOpeningFloatAndCashPayments() {
        AppUser employee = new AppUser();
        employee.setUsername("sales01");
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setStaff(employee);
        Attendance record = new Attendance();
        record.setAssignment(assignment);
        record.setCheckIn(Instant.now().minusSeconds(3600));
        record.setOpeningCash(new BigDecimal("200000"));

        when(assignments.findByIdAndStaff_Username(any(), any()))
                .thenReturn(Optional.of(assignment));
        when(attendance.findByAssignment_Id(any())).thenReturn(Optional.of(record));
        when(orders.countByHandledBy_IdAndChannelAndStatus(any(), any(), any())).thenReturn(0L);
        when(payments.sumCashPaymentsReceivedByBetween(any(), any(), any(), any())).thenReturn(new BigDecimal("500000"));
        when(handovers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkOut(1L, null, new BigDecimal("650000"), "Đã kiểm đếm", "sales01");

        ArgumentCaptor<ShiftHandover> captor = ArgumentCaptor.forClass(ShiftHandover.class);
        verify(handovers).save(captor.capture());
        ShiftHandover handover = captor.getValue();
        assertEquals(new BigDecimal("700000"), handover.getExpectedCash());
        assertEquals(new BigDecimal("650000"), handover.getHandedCash());
        assertEquals(new BigDecimal("-50000"), handover.getCashVariance());
        assertEquals("CLOSED", handover.getStatus());
        assertNotNull(record.getCheckOut());
        assertEquals("COMPLETED", assignment.getStatus());
    }
}
