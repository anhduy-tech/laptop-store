package com.lapstore.staff.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.PaymentRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.Attendance;
import com.lapstore.staff.domain.ShiftAssignment;
import com.lapstore.staff.domain.ShiftHandover;
import com.lapstore.staff.dto.AttendanceShiftView;
import com.lapstore.staff.dto.ShiftHandoverView;
import com.lapstore.staff.repo.AppUserRepository;
import com.lapstore.staff.repo.AttendanceRepository;
import com.lapstore.staff.repo.ShiftAssignmentRepository;
import com.lapstore.staff.repo.ShiftHandoverRepository;

@Service
class StaffAttendanceServiceImpl implements StaffAttendanceService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String STAFF_PORTAL = "hasAnyAuthority('PORTAL_SALES', 'PORTAL_CONSULT', 'PORTAL_WAREHOUSE')";
    private static final String SALES = "hasAuthority('PORTAL_SALES')";

    private final ShiftAssignmentRepository assignments;
    private final AttendanceRepository attendance;
    private final ShiftHandoverRepository handovers;
    private final AppUserRepository users;
    private final PaymentRepository payments;
    private final CustomerOrderRepository orders;

    StaffAttendanceServiceImpl(ShiftAssignmentRepository assignments, AttendanceRepository attendance,
            ShiftHandoverRepository handovers, AppUserRepository users, PaymentRepository payments,
            CustomerOrderRepository orders) {
        this.assignments = assignments;
        this.attendance = attendance;
        this.handovers = handovers;
        this.users = users;
        this.payments = payments;
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(STAFF_PORTAL)
    public List<AttendanceShiftView> today(String username) {
        LocalDate today = LocalDate.now(ZONE);
        var activeRecord = attendance.findFirstByAssignment_Staff_UsernameAndCheckInIsNotNullAndCheckOutIsNullOrderByCheckInDesc(username);
        boolean hasActiveAttendance = activeRecord.isPresent();
        List<ShiftAssignment> daily = new ArrayList<>(
                assignments.findByStaff_UsernameAndWorkDateAndStatusNotOrderByIdAsc(username, today, "CANCELLED"));
        activeRecord.map(Attendance::getAssignment)
                .filter(active -> daily.stream().noneMatch(a -> a.getId().equals(active.getId())))
                .ifPresent(daily::add);
        return daily
                .stream().map(assignment -> {
                    Attendance record = attendance.findByAssignment_Id(assignment.getId()).orElse(null);
                    boolean checkedIn = record != null && record.getCheckIn() != null;
                    boolean checkedOut = record != null && record.getCheckOut() != null;
                    List<IdName> receivers = users.findByBranch_IdAndActiveTrueAndDeletedAtIsNullOrderByFullNameAsc(
                                    assignment.getBranch().getId()).stream()
                            .filter(user -> !user.getUsername().equals(username))
                            .map(user -> new IdName(user.getId(), user.getFullName()))
                            .toList();
                    String status = checkedOut ? "Đã kết ca" : (checkedIn ? "Đang làm" : assignment.getStatus().equals("SCHEDULED") ? "Chưa vào ca" : assignment.getStatus());
                    return new AttendanceShiftView(assignment.getId(), DATE.format(assignment.getWorkDate()),
                            assignment.getShiftTemplate().getName(),
                            assignment.getBranch().getName(),
                            assignment.getShiftTemplate().getStartTime() + " – " + assignment.getShiftTemplate().getEndTime(),
                            status, checkedIn ? format(record.getCheckIn()) : null,
                            checkedOut ? format(record.getCheckOut()) : null,
                            record == null || record.getLateMinutes() == null ? 0 : record.getLateMinutes(),
                            !hasActiveAttendance && !checkedIn && assignment.getStatus().equals("SCHEDULED"),
                            checkedIn && !checkedOut, receivers);
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(STAFF_PORTAL)
    public List<ShiftHandoverView> pendingHandovers(String username) {
        return handovers.findByReceiver_UsernameAndStatusOrderByCreatedAtDesc(username, "PENDING")
                .stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(STAFF_PORTAL)
    public List<ShiftHandoverView> handoverHistory(String username) {
        PageRequest recent = PageRequest.of(0, 20);
        Map<Long, ShiftHandover> history = new LinkedHashMap<>();
        handovers.findByAssignment_Staff_UsernameOrderByCreatedAtDesc(username, recent)
                .forEach(handover -> history.put(handover.getId(), handover));
        handovers.findByReceiver_UsernameAndStatusNotOrderByCreatedAtDesc(username, "PENDING", recent)
                .forEach(handover -> history.put(handover.getId(), handover));
        return history.values().stream()
                .sorted(Comparator.comparing(ShiftHandover::getCreatedAt).reversed())
                .limit(20)
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize(STAFF_PORTAL)
    public void checkIn(long assignmentId, BigDecimal openingCash, String username) {
        BigDecimal opening = cashAmount(openingCash, "Tiền đầu ca");
        ShiftAssignment assignment = assignments
                .findByIdAndStaff_UsernameAndWorkDateAndStatus(assignmentId, username, LocalDate.now(ZONE), "SCHEDULED")
                .orElseThrow(() -> new BusinessException("Không tìm thấy ca làm được phân cho bạn hôm nay."));
        if (attendance.findByAssignment_Id(assignmentId).isPresent()) {
            throw new BusinessException("Ca này đã được chấm công.");
        }
        if (attendance.existsByAssignment_Staff_UsernameAndCheckInIsNotNullAndCheckOutIsNull(username)) {
            throw new BusinessException("Bạn đang có một ca chưa chấm ra.");
        }
        Instant now = Instant.now();
        LocalDateTime scheduledStart = assignment.getWorkDate()
                .atTime(LocalTime.parse(assignment.getShiftTemplate().getStartTime()));
        LocalDateTime checkedInAt = LocalDateTime.ofInstant(now, ZONE);
        int lateMinutes = (int) Math.max(0, Duration.between(scheduledStart, checkedInAt).toMinutes());

        Attendance record = new Attendance();
        record.setAssignment(assignment);
        record.setCheckIn(now);
        record.setLateMinutes(lateMinutes);
        record.setOpeningCash(opening);
        attendance.save(record);
    }

    @Override
    @Transactional
    @PreAuthorize(STAFF_PORTAL)
    public void checkOut(long assignmentId, Long receiverId, BigDecimal countedCash, String note, String username) {
        BigDecimal counted = cashAmount(countedCash, "Tiền bàn giao");
        ShiftAssignment assignment = assignments.findByIdAndStaff_Username(assignmentId, username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca làm."));
        Attendance record = attendance.findByAssignment_Id(assignmentId)
                .orElseThrow(() -> new BusinessException("Bạn chưa chấm công vào ca này."));
        if (record.getCheckIn() == null) throw new BusinessException("Bạn chưa chấm công vào ca này.");
        if (record.getCheckOut() != null) throw new BusinessException("Ca này đã được chấm ra.");
        if (orders.countByHandledBy_IdAndChannelAndStatus(assignment.getStaff().getId(), "COUNTER", "DRAFT") > 0) {
            throw new BusinessException("Hãy hoàn tất hoặc đóng các đơn quầy đang mở trước khi kết ca.");
        }

        AppUser receiver = null;
        if (receiverId != null) {
            receiver = users.findByIdAndDeletedAtIsNull(receiverId)
                    .filter(AppUser::isActive)
                    .orElseThrow(() -> new BusinessException("Nhân viên nhận bàn giao không hợp lệ."));
            if (receiver.getId().equals(assignment.getStaff().getId())
                    || receiver.getBranch() == null
                    || !receiver.getBranch().getId().equals(assignment.getBranch().getId())) {
                throw new BusinessException("Người nhận phải là nhân viên khác thuộc cùng chi nhánh.");
            }
        }

        Instant now = Instant.now();
        BigDecimal cashSales = payments.sumCashPaymentsReceivedByBetween(
                assignment.getStaff().getId(), assignment.getId(), record.getCheckIn(), now);
        BigDecimal expected = record.getOpeningCash().add(cashSales == null ? BigDecimal.ZERO : cashSales);
        ShiftHandover handover = new ShiftHandover();
        handover.setAssignment(assignment);
        handover.setReceiver(receiver);
        handover.setExpectedCash(expected);
        handover.setHandedCash(counted);
        handover.setCashVariance(counted.subtract(expected));
        handover.setNote(blankToNull(note));
        handover.setStatus(receiver == null ? "CLOSED" : "PENDING");
        handovers.save(handover);

        record.setCheckOut(now);
        assignment.setStatus("COMPLETED");
    }

    @Override
    @Transactional
    @PreAuthorize(STAFF_PORTAL)
    public void confirmHandover(long handoverId, BigDecimal receivedCash, String note, String username) {
        BigDecimal received = cashAmount(receivedCash, "Tiền thực nhận");
        ShiftHandover handover = handovers.findByIdAndReceiver_Username(handoverId, username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn giao dành cho bạn."));
        if (!"PENDING".equals(handover.getStatus())) {
            throw new BusinessException("Bàn giao này đã được xác nhận hoặc đóng.");
        }
        AppUser receiver = users.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản nhân viên."));
        handover.setReceivedCash(received);
        handover.setReceiveVariance(received.subtract(handover.getHandedCash()));
        handover.setReceiverNote(blankToNull(note));
        handover.setConfirmedBy(receiver);
        handover.setConfirmedAt(Instant.now());
        handover.setStatus("CONFIRMED");
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(SALES)
    public ShiftAssignment requireActiveAssignment(String username) {
        return attendance.findFirstByAssignment_Staff_UsernameAndAssignment_WorkDateAndCheckInIsNotNullAndCheckOutIsNull(
                        username, LocalDate.now(ZONE))
                .map(Attendance::getAssignment)
                .orElseThrow(() -> new BusinessException("Chấm công vào ca trước khi mở đơn quầy."));
    }

    private ShiftHandoverView view(ShiftHandover handover) {
        ShiftAssignment assignment = handover.getAssignment();
        return new ShiftHandoverView(handover.getId(), assignment.getStaff().getFullName(),
                assignment.getShiftTemplate().getName(), assignment.getBranch().getName(),
                handover.getReceiver() == null ? null : handover.getReceiver().getFullName(),
                format(handover.getCreatedAt()), handover.getExpectedCash(), handover.getHandedCash(),
                handover.getCashVariance(), handover.getReceivedCash(), handover.getReceiveVariance(),
                handover.getNote(), handover.getReceiverNote(), switch (handover.getStatus()) {
                    case "PENDING" -> "Chờ xác nhận";
                    case "CONFIRMED" -> "Đã xác nhận";
                    case "CLOSED" -> "Đã chốt";
                    default -> handover.getStatus();
                });
    }

    private static BigDecimal cashAmount(BigDecimal amount, String label) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2 || amount.precision() > 15) {
            throw new BusinessException(label + " phải là số tiền không âm, tối đa 2 chữ số thập phân.");
        }
        return amount;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }

    private static String format(Instant instant) {
        return instant == null ? null : TIME.format(LocalDateTime.ofInstant(instant, ZONE));
    }
}
