package com.lapstore.web.admin;

import com.lapstore.catalog.service.PriceHistoryService;
import com.lapstore.reporting.service.ReportingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** AD-00 dashboard. Số liệu lỗi không được làm sập trang: thiếu thì hiện 0. */
@Controller
class AdminDashboardController {
    private final ReportingService reports;
    private final PriceHistoryService prices;

    AdminDashboardController(ReportingService reports, PriceHistoryService prices) {
        this.reports = reports;
        this.prices = prices;
    }

    @GetMapping({"/admin", "/admin/"})
    String index(Model model) {
        try {
            model.addAttribute("kpi", reports.todayKpi());
            model.addAttribute("pendingPrices", prices.pendingCount());
        } catch (RuntimeException e) {
            model.addAttribute("pendingPrices", 0);
        }
        return "admin/index";
    }
}
