package com.lapstore.web.sales;

import java.security.Principal;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.HoldResult;
import com.lapstore.inventory.service.StockHoldService;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.service.CounterCheckoutService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SA-01 bán quầy: mở đơn → giữ serial → thu tiền & hoàn tất. Quyền cổng PORTAL_SALES do SecurityConfig kiểm. */
@Controller
@RequestMapping("/staff/sales")
class CounterOrderController {
    private final CounterCheckoutService counter;
    private final StockHoldService holds;

    CounterOrderController(CounterCheckoutService counter, StockHoldService holds) {
        this.counter = counter;
        this.holds = holds;
    }

    @GetMapping({"", "/"})
    String home() {
        return "redirect:/staff/sales/counter";
    }

    @GetMapping("/counter")
    String counter(Principal principal, Model model) {
        counter.current(principal.getName()).ifPresent(o -> model.addAttribute("order", o));
        return "sales/counter";
    }

    @PostMapping("/counter/open")
    String open(Principal principal, RedirectAttributes ra) {
        CounterOrderView order = counter.open(principal.getName());
        ra.addFlashAttribute("success", "Đã mở đơn " + order.code() + ".");
        return "redirect:/staff/sales/counter";
    }

    @PostMapping("/counter/hold")
    String hold(@RequestParam String serial, Principal principal, RedirectAttributes ra) {
        String sn = Text.blankToNull(serial);
        if (sn == null) throw new BusinessException("Vui lòng quét hoặc nhập serial.");
        CounterOrderView order = counter.current(principal.getName())
                .orElseThrow(() -> new BusinessException("Chưa có đơn đang mở — bấm “Mở đơn mới” trước."));
        HoldResult r = holds.hold(sn, order.id(), principal.getName());
        ra.addFlashAttribute("success", "Đã giữ " + r.serial() + " · " + r.variantName() + ".");
        return "redirect:/staff/sales/counter";
    }

    @PostMapping("/counter/complete")
    String complete(@RequestParam(required = false) String customerName,
                    @RequestParam(required = false) String phone,
                    Principal principal, RedirectAttributes ra) {
        CounterOrderView order = counter.current(principal.getName())
                .orElseThrow(() -> new BusinessException("Chưa có đơn đang mở."));
        if (order.lines().isEmpty()) throw new BusinessException("Đơn chưa có máy nào.");
        String code = counter.complete(order.id(), Text.blankToNull(customerName), Text.blankToNull(phone), principal.getName());
        ra.addFlashAttribute("success", "Đã hoàn tất đơn " + code + ".");
        return "redirect:/staff/sales/counter";
    }
}
