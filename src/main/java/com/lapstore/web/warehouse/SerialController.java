package com.lapstore.web.warehouse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lapstore.catalog.service.ProductAdminService;
import com.lapstore.common.web.Pages;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.service.SerialLookupService;

/**
 * WH-09 tra cứu serial (danh sách kết quả + hồ sơ một máy) và ô tìm nhanh gộp sản phẩm và serial.
 * Serial truyền bằng tham số `sn` (không đặt trong đường dẫn) vì có thể chứa ký tự đặc biệt như dấu gạch chéo.
 */
@Controller
@RequestMapping("/staff/warehouse")
class SerialController {
    private static final Set<String> STATUSES = Set.of("IN_STOCK", "IN_TRANSIT", "SOLD", "WARRANTY", "RETURNED",
            "DEFECTIVE");

    private final SerialLookupService serials;
    private final ProductAdminService products;

    SerialController(SerialLookupService serials, ProductAdminService products) {
        this.serials = serials;
        this.products = products;
    }

    @GetMapping("/serials")
    String list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String kw = Text.blankToNull(q);
        String st = status != null && STATUSES.contains(status) ? status : null;
        model.addAttribute("page", serials.search(kw, st, PageRequest.of(Math.max(page, 0), Pages.SIZE)));
        model.addAttribute("q", kw);
        model.addAttribute("status", st == null ? "" : st);
        model.addAttribute("tooShort", kw != null && kw.length() < SerialLookupService.MIN_QUERY);
        return "warehouse/serials";
    }

    @GetMapping("/serials/view")
    String detail(@RequestParam String sn, Model model) {
        model.addAttribute("view", serials.detail(sn));
        return "warehouse/serial-detail";
    }

    /** Ô tìm nhanh: serial trùng khớp hoàn toàn thì vào thẳng hồ sơ máy (quét mã vạch), ngược lại hiện cả sản phẩm và serial. */
    @GetMapping("/search")
    String search(@RequestParam(required = false) String q, Model model) {
        String kw = Text.blankToNull(q);
        if (kw == null)
            return "redirect:/staff/warehouse/products";
        var exact = serials.findExact(kw);
        if (exact.isPresent())
            return "redirect:/staff/warehouse/serials/view?sn=" + URLEncoder.encode(exact.get(), StandardCharsets.UTF_8);
        model.addAttribute("q", kw);
        model.addAttribute("productPage", products.search(kw, null, null, PageRequest.of(0, 5)));
        model.addAttribute("serialPage", serials.search(kw, null, PageRequest.of(0, 8)));
        return "warehouse/search-results";
    }
}
