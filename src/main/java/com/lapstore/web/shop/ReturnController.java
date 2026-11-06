package com.lapstore.web.shop;

import jakarta.validation.Valid;

import com.lapstore.aftersales.dto.ReturnCommand;
import com.lapstore.aftersales.service.AftersalesService;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SH-07 yêu cầu đổi trả. */
@Controller
@RequestMapping("/shop/returns")
class ReturnController {
    private final AftersalesService aftersales;

    ReturnController(AftersalesService aftersales) { this.aftersales = aftersales; }

    @GetMapping
    String form(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new ReturnForm());
        return "aftersales/return-form";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") ReturnForm form, BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("errors", FormErrors.of(br));
            return "redirect:/shop/returns";
        }
        try {
            aftersales.createReturnRequest(new ReturnCommand(form.getOrderCode().trim(), form.getPhone().trim(),
                    form.getType(), form.getSerial().trim(), form.getReason().trim()));
        } catch (BusinessException e) {
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/returns";
        }
        ra.addFlashAttribute("success", "Đã gửi yêu cầu. Nhân viên sẽ liên hệ bạn sớm nhất.");
        return "redirect:/shop/returns";
    }
}
