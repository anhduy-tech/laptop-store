package com.lapstore.staff.repo;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.staff.domain.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch" })
    Optional<Attendance> findByAssignment_Id(Long assignmentId);

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch" })
    Optional<Attendance> findFirstByAssignment_Staff_UsernameAndCheckInIsNotNullAndCheckOutIsNullOrderByCheckInDesc(
            String username);

    boolean existsByAssignment_Staff_UsernameAndCheckInIsNotNullAndCheckOutIsNull(String username);

    @Query("""
            select count(distinct a.assignment.staff.id)
            from Attendance a
            where a.assignment.workDate = :workDate
              and a.assignment.status <> 'CANCELLED'
              and a.checkIn is not null
              and a.checkOut is null
            """)
    long countCurrentlyOnDuty(@Param("workDate") LocalDate workDate);
}
