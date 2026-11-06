package com.lapstore.reporting.service;

import java.time.LocalDate;

import com.lapstore.reporting.dto.DashboardKpi;
import com.lapstore.reporting.dto.SalesReport;

/** Báo cáo (AD-00, AD-04). Cần quyền REPORT_VIEW. branchId = null → tất cả chi nhánh. */
public interface ReportingService {
    DashboardKpi todayKpi();

    SalesReport dailySales(LocalDate from, LocalDate to, Long branchId);

    /** File .xlsx (Apache POI) của cùng báo cáo (mục 4.15). */
    byte[] exportDailySalesXlsx(LocalDate from, LocalDate to, Long branchId);
}
