package com.lapstore.web.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.sales.dto.CartView;
import com.lapstore.sales.dto.CheckoutRequest;
import com.lapstore.sales.service.CartService;
import com.lapstore.sales.service.OnlineOrderService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** SH-04 checkout -> SH-06 thành công. Lỗi form/voucher/tồn kho quay lại SH-04 và giữ nguyên dữ liệu đã nhập. */
@Controller
@RequestMapping("/shop/checkout")
class CheckoutController {
    private final CartService carts;
    private final OnlineOrderService orders;

    CheckoutController(CartService carts, OnlineOrderService orders) {
        this.carts = carts;
        this.orders = orders;
    }

    @GetMapping
    String form(HttpSession session, Model model, RedirectAttributes ra) {
        CartView cart = carts.view(CartSession.token(session));
        if (cart.items().isEmpty()) {
            ra.addFlashAttribute("error", "Giỏ hàng đang trống.");
            return "redirect:/shop/cart";
        }
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        if (!model.containsAttribute("form")) model.addAttribute("form", new CheckoutForm());
        return "shop/checkout";
    }

    @PostMapping
    String place(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult br,
                 HttpSession session, RedirectAttributes ra) {
        boolean delivery = "DELIVERY".equals(form.getReceiveMethod());
        if (delivery && Text.blankToNull(form.getAddress()) == null) {
            br.addError(FormErrors.error("form", "address", form.getAddress(), "Vui lòng nhập địa chỉ giao hàng"));
        }
        if (br.hasErrors()) return backWith(form, FormErrors.of(br), null, ra);

        try {
            String code = orders.placeOrder(CartSession.token(session), new CheckoutRequest(
                    form.getFullName().trim(), form.getPhone().trim(), Text.blankToNull(form.getEmail()),
                    form.getReceiveMethod(), delivery ? form.getAddress().trim() : null,
                    Text.blankToNull(form.getVoucherCode())));
            ra.addFlashAttribute("orderCode", code);
            return "redirect:/shop/orders/success";
        } catch (BusinessException e) {
            // voucher sai / hết hàng: hiện flash error, giữ form
            return backWith(form, null, e.getMessage(), ra);
        }
    }

    private String backWith(CheckoutForm form, java.util.Map<String, String> errors, String error, RedirectAttributes ra) {
        ra.addFlashAttribute("form", form);
        if (errors != null) ra.addFlashAttribute("errors", errors);
        if (error != null) ra.addFlashAttribute("error", error);
        return "redirect:/shop/checkout";
    }
}
