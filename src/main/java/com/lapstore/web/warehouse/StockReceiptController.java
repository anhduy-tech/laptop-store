package com.lapstore.web.warehouse;

import java.security.Principal;

import jakarta.validation.Valid;

import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** WH-01 nhập kho (lưu nháp / xác nhận). */
@Controller
@RequestMapping("/staff/warehouse/receipts")
class StockReceiptController {
    private final InventoryService inventory;

    StockReceiptController(InventoryService inventory) { this.inventory = inventory; }

    @GetMapping
    String form(Model model) {
        model.addAttribute("suppliers", inventory.suppliers());
        model.addAttribute("branches", inventory.branches());
        return "warehouse/stock-receipt";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") StockReceiptForm form, BindingResult br,
                  Principal principal, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/staff/warehouse/receipts";
        }
        boolean confirm = "confirm".equals(form.getAction());
        String code = inventory.createReceipt(new StockReceiptRequest(form.getSupplierId(), form.getBranchId(),
                Text.blankToNull(form.getNote()), form.getLines(), confirm), principal.getName());
        ra.addFlashAttribute("success", (confirm ? "Đã xác nhận nhập kho " : "Đã lưu nháp phiếu ") + code + ".");
        return "redirect:/staff/warehouse/receipts";
    }
}
