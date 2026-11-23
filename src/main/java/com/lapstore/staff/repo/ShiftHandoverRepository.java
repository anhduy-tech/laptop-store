package com.lapstore.staff.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.ShiftHandover;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch", "receiver" })
    List<ShiftHandover> findByReceiver_UsernameAndStatusOrderByCreatedAtDesc(String username, String status);

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch", "receiver" })
    Page<ShiftHandover> findByReceiver_UsernameAndStatusNotOrderByCreatedAtDesc(
            String username, String status, Pageable pageable);

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch", "receiver" })
    Page<ShiftHandover> findByAssignment_Staff_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    @EntityGraph(attributePaths = { "assignment", "assignment.staff", "assignment.shiftTemplate", "assignment.branch", "receiver" })
    Optional<ShiftHandover> findByIdAndReceiver_Username(Long id, String username);
}
