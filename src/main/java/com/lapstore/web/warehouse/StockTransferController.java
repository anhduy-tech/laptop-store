package com.lapstore.web.warehouse;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.StockTransferRequest;
import com.lapstore.inventory.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** WH-02 chuyển kho theo danh sách serial. */
@Controller
@RequestMapping("/staff/warehouse/transfers")
class StockTransferController {
    private final InventoryService inventory;

    StockTransferController(InventoryService inventory) { this.inventory = inventory; }

    @GetMapping
    String form(Model model) {
        model.addAttribute("branches", inventory.branches());
        return "warehouse/transfer";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") TransferForm form, BindingResult br,
                  Principal principal, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/staff/warehouse/transfers";
        }
        if (form.getFromBranchId().equals(form.getToBranchId())) {
            throw new BusinessException("Chi nhánh nguồn và đích phải khác nhau.");
        }
        List<String> serials = Text.lines(form.getSerials());
        if (serials.isEmpty()) throw new BusinessException("Nhập ít nhất một serial.");
        String code = inventory.transfer(
                new StockTransferRequest(form.getFromBranchId(), form.getToBranchId(), serials), principal.getName());
        ra.addFlashAttribute("success", "Đã chuyển " + serials.size() + " máy (phiếu " + code + ").");
        return "redirect:/staff/warehouse/transfers";
    }
}
