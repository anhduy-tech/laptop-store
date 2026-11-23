package com.lapstore.reporting.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.reporting.dto.DashboardKpi;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.staff.repo.AttendanceRepository;

@Service
@Transactional(readOnly = true)
public class DashboardKpiService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CustomerOrderRepository orders;
    private final AttendanceRepository attendance;

    public DashboardKpiService(CustomerOrderRepository orders, AttendanceRepository attendance) {
        this.orders = orders;
        this.attendance = attendance;
    }

    public DashboardKpi today() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        var start = today.atStartOfDay(BUSINESS_ZONE).toInstant();
        var end = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        return new DashboardKpi(
                orders.sumCompletedRevenueBetween(start, end).longValue(),
                Math.toIntExact(orders.countByPlacedAtGreaterThanEqualAndPlacedAtLessThanAndStatusNot(
                        start, end, "DRAFT")),
                Math.toIntExact(attendance.countCurrentlyOnDuty(today)));
    }
}
