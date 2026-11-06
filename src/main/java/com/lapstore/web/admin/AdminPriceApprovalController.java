package com.lapstore.web.admin;

import java.security.Principal;

import com.lapstore.catalog.service.PriceHistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** AD-03 duyệt / từ chối đề xuất giá. */
@Controller
@RequestMapping("/admin/prices")
class AdminPriceApprovalController {
    private final PriceHistoryService prices;

    AdminPriceApprovalController(PriceHistoryService prices) { this.prices = prices; }

    @GetMapping
    String list(Model model) {
        model.addAttribute("proposals", prices.pending());
        return "admin/price-approve";
    }

    @PostMapping("/{id}/approve")
    String approve(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        prices.approve(id, principal.getName());
        ra.addFlashAttribute("success", "Đã duyệt giá mới.");
        return "redirect:/admin/prices";
    }

    @PostMapping("/{id}/reject")
    String reject(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        prices.reject(id, principal.getName());
        ra.addFlashAttribute("success", "Đã từ chối đề xuất.");
        return "redirect:/admin/prices";
    }
}
