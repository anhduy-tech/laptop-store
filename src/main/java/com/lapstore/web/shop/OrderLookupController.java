package com.lapstore.web.shop;

import com.lapstore.common.web.Text;
import com.lapstore.sales.service.OrderQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** SH-05 tra cứu đơn · SH-06 đặt hàng thành công. */
@Controller
@RequestMapping("/shop/orders")
class OrderLookupController {
    private final OrderQueryService orders;

    OrderLookupController(OrderQueryService orders) { this.orders = orders; }

    @GetMapping("/lookup")
    String lookup(@RequestParam(required = false) String code,
                  @RequestParam(required = false) String phone, Model model) {
        String c = Text.orEmpty(code);
        String p = Text.orEmpty(phone);
        model.addAttribute("code", c);
        model.addAttribute("phone", p);
        if (!c.isEmpty() && !p.isEmpty()) {
            orders.lookup(c, p).ifPresentOrElse(
                    o -> model.addAttribute("order", o),
                    () -> model.addAttribute("notFound", true));
        }
        return "shop/order-lookup";
    }

    /** orderCode đến từ flash của POST /shop/checkout; vào thẳng URL này thì quay về shop. */
    @GetMapping("/success")
    String success(Model model) {
        return model.containsAttribute("orderCode") ? "shop/order-success" : "redirect:/shop";
    }
}
