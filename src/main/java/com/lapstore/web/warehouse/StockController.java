package com.lapstore.web.warehouse;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lapstore.common.web.Text;
import com.lapstore.inventory.service.InventoryService;
import com.lapstore.inventory.service.StockQueryService;

/** WH-06 tồn kho theo chi nhánh và danh sách serial của từng cấu hình. */
@Controller
@RequestMapping("/staff/warehouse/stock")
class StockController {
    private static final int PAGE_SIZE = 25;
    private static final java.util.Set<String> SERIAL_STATUSES = java.util.Set.of("IN_STOCK", "IN_TRANSIT", "SOLD",
            "WARRANTY", "RETURNED", "DEFECTIVE");

    private final StockQueryService stock;
    private final InventoryService inventory;

    StockController(StockQueryService stock, InventoryService inventory) {
        this.stock = stock;
        this.inventory = inventory;
    }

    @GetMapping
    String list(@RequestParam(required = false) String q, @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "false") boolean low, @RequestParam(defaultValue = "0") int page,
            Model model) {
        String qq = Text.blankToNull(q);
        model.addAttribute("page", stock.search(qq, branchId, low, PageRequest.of(Math.max(page, 0), PAGE_SIZE)));
        model.addAttribute("branches", inventory.branches());
        model.addAttribute("q", qq);
        model.addAttribute("selectedBranch", branchId);
        model.addAttribute("low", low);
        return "warehouse/stock";
    }

    @GetMapping("/{variantId}/{branchId}")
    String serials(@PathVariable long variantId, @PathVariable long branchId,
            @RequestParam(required = false) String status, Model model) {
        String st = Text.blankToNull(status);
        if (st != null && !SERIAL_STATUSES.contains(st))
            st = null;
        model.addAttribute("view", stock.serials(variantId, branchId, st));
        model.addAttribute("selectedStatus", st);
        return "warehouse/stock-serials";
    }
}
