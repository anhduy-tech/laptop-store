package com.lapstore.web.warehouse;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.web.Pages;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.AdjustResult;
import com.lapstore.inventory.dto.ScanResult;
import com.lapstore.inventory.service.InventoryService;
import com.lapstore.inventory.service.StocktakeService;

/**
 * WH-08 kiểm kê kho theo chi nhánh. Quyền xem PORTAL_WAREHOUSE, thao tác STOCK_STOCKTAKE (kiểm ở service); lỗi nghiệp vụ do
 * GlobalExceptionHandler đưa về trang trước kèm thông báo.
 */
@Controller
@RequestMapping("/staff/warehouse/stocktakes")
class StocktakeController {
    private static final java.util.Set<String> STATUSES = java.util.Set.of("OPEN", "CLOSED", "CANCELLED");
    private static final java.util.Set<String> FILTERS = java.util.Set.of("MATCHED", "PENDING", "UNEXPECTED");

    private final StocktakeService stocktakes;
    private final InventoryService inventory;

    StocktakeController(StocktakeService stocktakes, InventoryService inventory) {
        this.stocktakes = stocktakes;
        this.inventory = inventory;
    }

    @GetMapping
    String list(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int page, Model model) {
        String st = STATUSES.contains(status) ? status : null;
        model.addAttribute("page", stocktakes.list(st, PageRequest.of(Math.max(page, 0), Pages.SIZE)));
        model.addAttribute("status", st == null ? "" : st);
        return "warehouse/stocktake-list";
    }

    @GetMapping("/new")
    String form(Model model) {
        model.addAttribute("branches", inventory.branches());
        return "warehouse/stocktake-new";
    }

    @PostMapping
    String start(@RequestParam Long branchId, @RequestParam(required = false) String scope, Principal principal,
            RedirectAttributes ra) {
        String code = stocktakes.start(branchId, Text.blankToNull(scope), principal.getName());
        ra.addFlashAttribute("success", "Đã tạo phiên kiểm kê " + code + ". Quét serial các máy thực tế đang có ở chi nhánh.");
        return "redirect:/staff/warehouse/stocktakes/" + code;
    }

    @GetMapping("/{code}")
    String detail(@PathVariable String code, @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String f = filter != null && FILTERS.contains(filter) ? filter : null;
        var view = stocktakes.get(code, f, PageRequest.of(Math.max(page, 0), Pages.SIZE));
        model.addAttribute("view", view);
        model.addAttribute("itemPage", view.items());
        model.addAttribute("filter", f == null ? "" : f);
        return "warehouse/stocktake-detail";
    }

    @PostMapping("/{code}/scan")
    String scan(@PathVariable String code, @RequestParam String serials, Principal principal, RedirectAttributes ra) {
        ScanResult r = stocktakes.scan(code, splitLines(serials), principal.getName());
        StringBuilder ok = new StringBuilder("Đã ghi nhận ").append(r.recorded()).append(" máy");
        if (r.unexpected() > 0)
            ok.append(" (").append(r.unexpected()).append(" máy dư, không thuộc danh sách của chi nhánh)");
        ok.append('.');
        if (r.already() > 0)
            ok.append(' ').append(r.already()).append(" máy đã quét trước đó.");
        ra.addFlashAttribute("success", ok.toString());
        if (!r.unknown().isEmpty()) {
            List<String> sample = r.unknown().subList(0, Math.min(8, r.unknown().size()));
            ra.addFlashAttribute("error", "Không có trong hệ thống (" + r.unknown().size() + " serial): "
                    + String.join(", ", sample) + (r.unknown().size() > sample.size() ? "..." : "")
                    + ". Các serial này không được ghi nhận.");
        }
        return "redirect:/staff/warehouse/stocktakes/" + code;
    }

    @PostMapping("/{code}/close")
    String close(@PathVariable String code, Principal principal, RedirectAttributes ra) {
        stocktakes.close(code, principal.getName());
        ra.addFlashAttribute("success", "Đã đóng phiên kiểm kê " + code + ". Máy chưa quét được ghi là thiếu.");
        return "redirect:/staff/warehouse/stocktakes/" + code;
    }

    @PostMapping("/{code}/cancel")
    String cancel(@PathVariable String code, Principal principal, RedirectAttributes ra) {
        stocktakes.cancel(code, principal.getName());
        ra.addFlashAttribute("success", "Đã hủy phiên kiểm kê " + code + ".");
        return "redirect:/staff/warehouse/stocktakes/" + code;
    }

    @PostMapping("/{code}/adjust")
    String adjust(@PathVariable String code, Principal principal, RedirectAttributes ra) {
        AdjustResult r = stocktakes.adjust(code, principal.getName());
        ra.addFlashAttribute("success", "Đã điều chỉnh: " + r.relocated() + " máy dư chuyển về chi nhánh, " + r.writtenOff()
                + " máy thiếu chuyển DEFECTIVE và trừ tồn.");
        if (!r.skipped().isEmpty()) {
            List<String> sample = r.skipped().subList(0, Math.min(5, r.skipped().size()));
            ra.addFlashAttribute("error", r.skipped().size() + " dòng không điều chỉnh được: "
                    + String.join("; ", sample) + (r.skipped().size() > sample.size() ? "..." : ""));
        }
        return "redirect:/staff/warehouse/stocktakes/" + code;
    }

    private static List<String> splitLines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
