package com.lapstore.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.lapstore.reporting.dto.DashboardKpi;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.staff.repo.AttendanceRepository;

class DashboardKpiServiceTest {
    private final CustomerOrderRepository orders = org.mockito.Mockito.mock(CustomerOrderRepository.class);
    private final AttendanceRepository attendance = org.mockito.Mockito.mock(AttendanceRepository.class);
    private final DashboardKpiService service = new DashboardKpiService(orders, attendance);

    @Test
    void todayUsesCompletedRevenueOrdersAndOpenAttendanceForVietnamBusinessDay() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        when(orders.sumCompletedRevenueBetween(start, end)).thenReturn(BigDecimal.valueOf(125_000_000));
        when(orders.countByPlacedAtGreaterThanEqualAndPlacedAtLessThanAndStatusNot(start, end, "DRAFT"))
                .thenReturn(7L);
        when(attendance.countCurrentlyOnDuty(today)).thenReturn(3L);

        assertThat(service.today()).isEqualTo(new DashboardKpi(125_000_000L, 7, 3));
        verify(orders).sumCompletedRevenueBetween(start, end);
        verify(orders).countByPlacedAtGreaterThanEqualAndPlacedAtLessThanAndStatusNot(start, end, "DRAFT");
        verify(attendance).countCurrentlyOnDuty(today);
    }
}
