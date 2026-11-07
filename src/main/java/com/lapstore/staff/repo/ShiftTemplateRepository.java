package com.lapstore.staff.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.ShiftTemplate;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
    List<ShiftTemplate> findByActiveTrueOrderByIdAsc();
}
