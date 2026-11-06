package com.lapstore.web.admin;

import java.time.LocalDate;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.service.InventoryService;
import com.lapstore.reporting.dto.SalesReport;
import com.lapstore.reporting.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** AD-04 báo cáo doanh thu + xuất Excel. Mặc định xem 7 ngày gần nhất. */
@Controller
@RequestMapping("/admin/reports")
class AdminReportController {
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportingService reports;
    private final InventoryService inventory;

    AdminReportController(ReportingService reports, InventoryService inventory) {
        this.reports = reports;
        this.inventory = inventory;
    }

    @GetMapping
    String view(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                @RequestParam(required = false) Long branchId, Model model) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(6);
        check(start, end);
        SalesReport r = reports.dailySales(start, end, branchId);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("branchId", branchId);
        model.addAttribute("branches", inventory.branches());
        model.addAttribute("rows", r.rows());
        model.addAttribute("totalOrders", r.totalOrders());
        model.addAttribute("totalRevenue", r.totalRevenue());
        model.addAttribute("totalProfit", r.totalProfit());
        return "admin/reports";
    }

    @GetMapping("/export.xlsx")
    ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) Long branchId) {
        check(from, to);
        byte[] file = reports.exportDailySalesXlsx(from, to, branchId);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bao-cao-" + from + "_" + to + ".xlsx").build().toString())
                .body(file);
    }

    private static void check(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new BusinessException("“Đến ngày” phải sau hoặc bằng “Từ ngày”.");
    }
}
