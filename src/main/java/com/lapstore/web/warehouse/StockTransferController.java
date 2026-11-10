package com.lapstore.web.warehouse;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.StockTransferRequest;
import com.lapstore.inventory.service.InventoryService;
import com.lapstore.inventory.service.StockQueryService;
import com.lapstore.common.web.Pages;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** WH-02 chuyển kho theo danh sách serial. */
@Controller
@RequestMapping("/staff/warehouse/transfers")
class StockTransferController {
    private final InventoryService inventory;
    private final StockQueryService stock;

    StockTransferController(InventoryService inventory, StockQueryService stock) {
        this.inventory = inventory;
        this.stock = stock;
    }

    @GetMapping
    String form(Model model) {
        model.addAttribute("branches", inventory.branches());
        return "warehouse/transfer";
    }

    /** Danh sách phiếu chuyển: lọc theo trạng thái, mặc định xem phiếu đang vận chuyển cần nhận. */
    @GetMapping("/list")
    String list(@RequestParam(defaultValue = "IN_TRANSIT") String status, @RequestParam(defaultValue = "0") int page,
            Model model) {
        String st = "IN_TRANSIT".equals(status) || "RECEIVED".equals(status) ? status : null;
        model.addAttribute("page", stock.transfers(st, PageRequest.of(Math.max(page, 0), Pages.SIZE)));
        model.addAttribute("status", st == null ? "" : st);
        return "warehouse/transfer-list";
    }

    @GetMapping("/{code}")
    String detail(@PathVariable String code, @RequestParam(defaultValue = "0") int page, Model model) {
        var view = stock.transfer(code);
        model.addAttribute("view", view);
        model.addAttribute("itemPage", Pages.of(view.items(), page, 15));
        return "warehouse/transfer-detail";
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
        ra.addFlashAttribute("success", "Đã xuất " + serials.size() + " máy, đang vận chuyển (phiếu " + code + "). Chi nhánh đích cần xác nhận nhận hàng.");
        return "redirect:/staff/warehouse/transfers";
    }

    @PostMapping("/{code}/receive")
    String receive(@PathVariable String code, Principal principal, RedirectAttributes ra) {
        inventory.receiveTransfer(code, principal.getName());
        ra.addFlashAttribute("success", "Đã nhận hàng phiếu " + code + ": tồn kho chi nhánh đích đã được cập nhật.");
        return "redirect:/staff/warehouse/transfers/" + code;
    }
}
