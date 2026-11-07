package com.lapstore.staff.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    @EntityGraph(attributePaths = { "staff", "shiftTemplate", "branch" })
    List<ShiftAssignment> findByWorkDateBetweenOrderByWorkDateAscIdAsc(LocalDate from, LocalDate to);

    /** Ca của một nhân viên trong ngày (trừ ca đã hủy), để kiểm tra trùng giờ. */
    @EntityGraph(attributePaths = "shiftTemplate")
    List<ShiftAssignment> findByStaff_IdAndWorkDateAndStatusNot(Long staffId, LocalDate workDate, String status);
}
