package com.lapstore.web.warehouse;

import java.security.Principal;

import jakarta.validation.Valid;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.sales.dto.CampaignRequest;
import com.lapstore.sales.service.CampaignService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.lapstore.common.web.Pages;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** WH-03 campaign & voucher. */
@Controller
@RequestMapping("/staff/warehouse/campaigns")
class CampaignController {
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
        return "warehouse/campaigns";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") CampaignForm form, BindingResult br,
                  Principal principal, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/staff/warehouse/campaigns";
        }
        if (form.getEndAt().isBefore(form.getStartAt())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if ("PERCENT".equals(form.getType()) && form.getValue() > 100) {
            throw new BusinessException("Giảm theo % không được quá 100.");
        }
        campaigns.createCampaign(new CampaignRequest(form.getName().trim(), form.getType(), form.getValue(),
                form.getStartAt(), form.getEndAt()), principal.getName());
        ra.addFlashAttribute("success", "Đã tạo campaign.");
        return "redirect:/staff/warehouse/campaigns";
    }
}
