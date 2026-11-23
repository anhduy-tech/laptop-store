package com.lapstore.web.admin;

import com.lapstore.catalog.service.PriceHistoryService;
import com.lapstore.reporting.service.DashboardKpiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** AD-00 dashboard. Số liệu lỗi không được làm sập trang: thiếu thì hiện 0. */
@Controller
class AdminDashboardController {
    private final DashboardKpiService dashboard;
    private final PriceHistoryService prices;

    AdminDashboardController(DashboardKpiService dashboard, PriceHistoryService prices) {
        this.dashboard = dashboard;
        this.prices = prices;
    }

    @GetMapping({"/admin", "/admin/"})
    String index(Model model) {
        model.addAttribute("kpi", dashboard.today());
        model.addAttribute("pendingPrices", prices.pendingCount());
        return "admin/index";
    }
}
