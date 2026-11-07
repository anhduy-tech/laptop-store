package com.lapstore.web.warehouse;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.catalog.dto.PriceProposalRequest;
import com.lapstore.catalog.service.PriceHistoryService;
import com.lapstore.common.web.FormErrors;

import jakarta.validation.Valid;

/** WH-04 đề xuất giá -> trạng thái PENDING chờ admin duyệt (AD-03). */
@Controller
@RequestMapping("/staff/warehouse/prices")
class PriceProposalController {
    private final PriceHistoryService prices;

    PriceProposalController(PriceHistoryService prices) { this.prices = prices; }

    @GetMapping
    String form() {
        return "warehouse/price-propose";
    }

    @PostMapping
    String propose(@Valid @ModelAttribute("form") PriceProposalForm form, BindingResult br,
                   Principal principal, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/staff/warehouse/prices";
        }
        prices.propose(new PriceProposalRequest(form.getVariantId(), form.getNewPrice(),
                form.getEffectiveFrom(), form.getReason().trim()), principal.getName());
        ra.addFlashAttribute("success", "Đã gửi đề xuất giá, chờ admin duyệt.");
        return "redirect:/staff/warehouse/prices";
    }
}
