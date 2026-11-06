package com.lapstore.web.sales;

import java.security.Principal;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.Text;
import com.lapstore.sales.service.OrderQueryService;
import com.lapstore.sales.service.OrderService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SA-02 danh sách đơn · SA-03 chi tiết đơn, gán serial, xác nhận / hoàn tất / hủy. */
@Controller
@RequestMapping("/staff/sales/orders")
class SalesOrderController {
    private final OrderQueryService queries;
    private final OrderService orders;

    SalesOrderController(OrderQueryService queries, OrderService orders) {
        this.queries = queries;
        this.orders = orders;
    }

    @GetMapping
    String list(@RequestParam(required = false) String status,
                @RequestParam(required = false) String q, Model model) {
        String st = Text.orEmpty(status);
        String kw = Text.orEmpty(q);
        model.addAttribute("statuses", queries.statuses());
        model.addAttribute("status", st);
        model.addAttribute("q", kw);
        model.addAttribute("orders", queries.search(st, kw));
        return "sales/order-list";
    }

    @GetMapping("/{id}")
    String detail(@PathVariable long id, Model model) {
        model.addAttribute("order", queries.detail(id));
        return "sales/order-detail";
    }

    @PostMapping("/{id}/items/{itemId}/serial")
    String assignSerial(@PathVariable long id, @PathVariable long itemId, @RequestParam String serial,
                        Principal principal, RedirectAttributes ra) {
        String sn = Text.blankToNull(serial);
        if (sn == null) throw new BusinessException("Vui lòng nhập serial.");
        orders.assignSerial(id, itemId, sn, principal.getName());
        ra.addFlashAttribute("success", "Đã gán serial " + sn + ".");
        return back(id);
    }

    @PostMapping("/{id}/confirm")
    String confirm(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        orders.confirm(id, principal.getName());
        ra.addFlashAttribute("success", "Đã xác nhận đơn.");
        return back(id);
    }

    @PostMapping("/{id}/complete")
    String complete(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        orders.complete(id, principal.getName());
        ra.addFlashAttribute("success", "Đã hoàn tất đơn.");
        return back(id);
    }

    @PostMapping("/{id}/cancel")
    String cancel(@PathVariable long id, @RequestParam String reason, Principal principal, RedirectAttributes ra) {
        String why = Text.blankToNull(reason);
        if (why == null) throw new BusinessException("Vui lòng nhập lý do hủy.");
        orders.cancel(id, why, principal.getName());
        ra.addFlashAttribute("success", "Đã hủy đơn.");
        return back(id);
    }

    /** Tải hóa đơn PDF (mục 4.15). */
    @GetMapping("/{id}/invoice.pdf")
    ResponseEntity<byte[]> invoice(@PathVariable long id) {
        byte[] pdf = queries.invoicePdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("hoa-don-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    private static String back(long id) {
        return "redirect:/staff/sales/orders/" + id;
    }
}
