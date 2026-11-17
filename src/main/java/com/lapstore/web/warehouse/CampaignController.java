package com.lapstore.web.warehouse;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Pages;
import com.lapstore.sales.dto.CampaignRequest;
import com.lapstore.sales.dto.VoucherRequest;
import com.lapstore.sales.service.CampaignService;

import jakarta.validation.Valid;

/** WH-03 campaign & voucher: danh sách, tạo mới, tạm dừng/bật lại. Campaign đang chạy được POS tự áp, voucher được gợi ý ở màn bán quầy. */
@Controller
@RequestMapping("/staff/warehouse/campaigns")
class CampaignController {
    private static final String LIST = "redirect:/staff/warehouse/campaigns";
    private final CampaignService campaigns;

    CampaignController(CampaignService campaigns) { this.campaigns = campaigns; }

    @GetMapping
    String list(@RequestParam(defaultValue = "0") int cpage, @RequestParam(defaultValue = "0") int vpage, Model model) {
        var cp = Pages.of(campaigns.campaigns(), cpage);
        var vp = Pages.of(campaigns.vouchers(), vpage);
        model.addAttribute("campaigns", cp.getContent());
        model.addAttribute("campaignsPage", cp);
        model.addAttribute("vouchers", vp.getContent());
        model.addAttribute("vouchersPage", vp);
        model.addAttribute("scope", campaigns.scopeOptions());
        if (!model.containsAttribute("form")) model.addAttribute("form", new CampaignForm());
        if (!model.containsAttribute("vform")) model.addAttribute("vform", new VoucherForm());
        return "warehouse/campaigns";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") CampaignForm f, BindingResult br, RedirectAttributes ra, Principal principal) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return LIST;
        }
        campaigns.createCampaign(new CampaignRequest(f.getName().trim(), f.getType(), f.getValue(), f.getMaxDiscount(),
                f.getMinOrder(), f.getPriority(), f.isStackable(), f.getStartAt(), f.getEndAt(), f.getScopeType(),
                f.getScopeTargetId()), principal.getName());
        ra.addFlashAttribute("success", "Đã tạo campaign.");
        return LIST;
    }

    @PostMapping("/{id}/toggle")
    String toggle(@PathVariable long id, @RequestParam boolean active, RedirectAttributes ra, Principal principal) {
        campaigns.setCampaignActive(id, active, principal.getName());
        ra.addFlashAttribute("success", active ? "Đã bật lại campaign." : "Đã tạm dừng campaign.");
        return LIST;
    }

    @PostMapping("/vouchers")
    String createVoucher(@Valid @ModelAttribute("vform") VoucherForm f, BindingResult br, RedirectAttributes ra, Principal principal) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return LIST;
        }
        campaigns.createVoucher(new VoucherRequest(f.getCode(), f.getName().trim(), f.getType(), f.getValue(), f.getMaxDiscount(),
                f.getMinOrder(), f.getTotalLimit(), f.getPerCustomerLimit(), f.isStackableWithCampaign(), f.getStartAt(),
                f.getEndAt()), principal.getName());
        ra.addFlashAttribute("success", "Đã tạo voucher " + f.getCode().trim().toUpperCase() + ".");
        return LIST;
    }

    @PostMapping("/vouchers/{id}/toggle")
    String toggleVoucher(@PathVariable long id, @RequestParam boolean active, RedirectAttributes ra, Principal principal) {
        campaigns.setVoucherActive(id, active, principal.getName());
        ra.addFlashAttribute("success", active ? "Đã bật lại voucher." : "Đã tạm dừng voucher.");
        return LIST;
    }
}
