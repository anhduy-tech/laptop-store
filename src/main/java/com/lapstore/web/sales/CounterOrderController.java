package com.lapstore.web.sales;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.HoldResult;
import com.lapstore.inventory.service.StockHoldService;
import com.lapstore.sales.dto.CounterOrderSummary;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.service.CounterCheckoutService;

/**
 * SA-01 bán quầy: nhiều đơn mở cùng lúc (mỗi đơn một tab trong trang) -> chọn khách, quét/chọn serial, voucher -> xem hóa đơn ->
 * thu tiền. Quyền cổng PORTAL_SALES do SecurityConfig kiểm, quyền chi tiết ở lớp service. Các API JSON cho popup ở
 * {@link CounterApiController}.
 */
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
    String counter(@RequestParam(required = false) Long order, Principal principal, Model model) {
        List<CounterOrderSummary> orders = counter.openOrders(principal.getName());
        model.addAttribute("orders", orders);
        model.addAttribute("maxOpen", CounterCheckoutService.MAX_OPEN);
        if (!orders.isEmpty()) {
            long id = order != null && orders.stream().anyMatch(o -> o.id().equals(order)) ? order : orders.get(0).id();
            CounterOrderView view = counter.view(id, principal.getName());
            model.addAttribute("order", view);
            model.addAttribute("suggestions", counter.voucherSuggestions(id, principal.getName()));
        }
        return "sales/counter";
    }

    @PostMapping("/counter/open")
    String open(Principal principal, RedirectAttributes ra) {
        CounterOrderView o = counter.open(principal.getName());
        ra.addFlashAttribute("success", "Đã mở đơn " + o.code() + ".");
        return back(o.id());
    }

    @PostMapping("/counter/{id}/hold")
    String hold(@PathVariable long id, @RequestParam String serial, Principal principal, RedirectAttributes ra) {
        String sn = Text.blankToNull(serial);
        if (sn == null) throw new BusinessException("Vui lòng quét hoặc nhập serial.");
        HoldResult r = holds.hold(sn, id, principal.getName());
        ra.addFlashAttribute("success", "Đã giữ " + r.serial() + " · " + r.variantName() + ".");
        return back(id);
    }

    /** Thêm nhiều máy từ popup chọn sản phẩm: từng máy một, máy nào lỗi thì báo lại, máy khác vẫn được giữ. */
    @PostMapping("/counter/{id}/hold-many")
    String holdMany(@PathVariable long id, @RequestParam(required = false) String serials, Principal principal,
                    RedirectAttributes ra) {
        List<String> list = Text.lines(serials);
        if (list.isEmpty()) throw new BusinessException("Chưa chọn máy nào.");
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (String sn : list) {
            try {
                holds.hold(sn, id, principal.getName());
                ok++;
            } catch (BusinessException e) {
                errors.add(sn + ": " + e.getMessage());
            }
        }
        if (ok > 0) ra.addFlashAttribute("success", "Đã thêm " + ok + " máy vào đơn.");
        if (!errors.isEmpty()) ra.addFlashAttribute("error", String.join(" · ", errors));
        return back(id);
    }

    @PostMapping("/counter/{id}/release")
    String release(@PathVariable long id, @RequestParam String serial, Principal principal, RedirectAttributes ra) {
        holds.release(serial.trim(), id, principal.getName());
        ra.addFlashAttribute("success", "Đã bỏ máy " + serial.trim() + " khỏi đơn.");
        return back(id);
    }

    @PostMapping("/counter/{id}/renew")
    String renew(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        int n = counter.renewHolds(id, principal.getName());
        ra.addFlashAttribute("success", "Đã gia hạn giữ " + n + " máy.");
        return back(id);
    }

    @PostMapping("/counter/{id}/cancel")
    String cancel(@PathVariable long id, @RequestParam(required = false) String reason, Principal principal,
                  RedirectAttributes ra) {
        counter.cancel(id, reason, principal.getName());
        ra.addFlashAttribute("success", "Đã đóng đơn, các máy đã được nhả.");
        return "redirect:/staff/sales/counter";
    }

    @PostMapping("/counter/{id}/customer")
    String customer(@PathVariable long id, @RequestParam(required = false) Long customerId, Principal principal,
                    RedirectAttributes ra) {
        counter.attachCustomer(id, customerId, principal.getName());
        ra.addFlashAttribute("success", customerId == null ? "Đã bỏ chọn khách (khách lẻ)." : "Đã chọn khách hàng.");
        return back(id);
    }

    @PostMapping("/counter/{id}/customer/quick")
    String quickCustomer(@PathVariable long id, @RequestParam(required = false) String name, @RequestParam String phone,
                         Principal principal, RedirectAttributes ra) {
        var c = counter.quickCustomer(id, Text.blankToNull(name), phone, principal.getName());
        ra.addFlashAttribute("success", "Đã chọn khách " + c.name() + " · " + c.phone() + ".");
        return back(id);
    }

    @PostMapping("/counter/{id}/voucher")
    String voucher(@PathVariable long id, @RequestParam String code, Principal principal, RedirectAttributes ra) {
        counter.applyVoucher(id, code, principal.getName());
        ra.addFlashAttribute("success", "Đã áp voucher " + code.trim().toUpperCase() + ".");
        return back(id);
    }

    @PostMapping("/counter/{id}/voucher/remove")
    String removeVoucher(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        counter.removeVoucher(id, principal.getName());
        ra.addFlashAttribute("success", "Đã gỡ voucher.");
        return back(id);
    }

    /** Xem trước hóa đơn + chọn phương thức thanh toán, trước khi chốt. */
    @GetMapping("/counter/{id}/confirm")
    String confirm(@PathVariable long id, Principal principal, Model model) {
        CounterOrderView view = counter.view(id, principal.getName());
        if (view.lines().isEmpty()) throw new BusinessException("Đơn chưa có máy nào.");
        model.addAttribute("order", view);
        return "sales/counter-confirm";
    }

    @PostMapping("/counter/{id}/complete")
    String complete(@PathVariable long id, @RequestParam(defaultValue = "CASH") String method,
                    @RequestParam(required = false) Long tendered, @RequestParam(required = false) String reference,
                    Principal principal, RedirectAttributes ra) {
        String code = counter.complete(id, method, tendered, Text.blankToNull(reference), principal.getName());
        ra.addFlashAttribute("success", "Đã hoàn tất đơn " + code + ".");
        return "redirect:/staff/sales/counter";
    }

    private static String back(long orderId) {
        return "redirect:/staff/sales/counter?order=" + orderId;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotFoundException.class)
    String gone(NotFoundException e, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/staff/sales/counter";
    }
}
