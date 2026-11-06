package com.lapstore.staff.dto;

import java.time.LocalDate;

public record ShiftAssignCommand(Long userId, Long templateId, Long branchId, LocalDate workDate) {}
