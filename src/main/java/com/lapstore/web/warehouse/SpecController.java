package com.lapstore.web.warehouse;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.dto.SpecOptionRow;
import com.lapstore.catalog.service.SpecService;
import com.lapstore.common.web.Pages;
import com.lapstore.common.web.Text;

/**
 * WH-07 quản lý thuộc tính CPU / GPU / RAM / ổ cứng: xem số sản phẩm đang dùng, thêm (chống trùng), sửa tên,
 * xóa khi không còn sản phẩm nào dùng. Quyền sửa PRODUCT_MANAGE được kiểm ở service; lỗi nghiệp vụ do
 * GlobalExceptionHandler đưa về trang này kèm thông báo.
 */
@Controller
@RequestMapping("/staff/warehouse/attributes")
class SpecController {
    private final SpecService specs;

    SpecController(SpecService specs) {
        this.specs = specs;
    }

    @GetMapping
    String list(@RequestParam(defaultValue = "CPU") SpecKind kind, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String kw = Text.blankToNull(q);
        List<SpecOptionRow> all = specs.list(kind);
        if (kw != null) {
            String needle = compact(kw);
            all = all.stream().filter(r -> compact(r.label()).contains(needle)).toList();
        }
        var p = Pages.of(all, page);
        model.addAttribute("kinds", SpecKind.values());
        model.addAttribute("kind", kind);
        model.addAttribute("q", kw);
        model.addAttribute("rows", p.getContent());
        model.addAttribute("rowsPage", p);
        return "warehouse/attributes";
    }

    /** Xem các sản phẩm/biến thể đang dùng một giá trị, trước khi sửa tên hoặc xóa. */
    @GetMapping("/{id}/usage")
    String usage(@PathVariable long id, @RequestParam(defaultValue = "0") int page, Model model) {
        var view = specs.usage(id, PageRequest.of(Math.max(page, 0), Pages.SIZE));
        model.addAttribute("view", view);
        model.addAttribute("rowsPage", view.rows());
        return "warehouse/attribute-usage";
    }

    @PostMapping
    String add(@RequestParam SpecKind kind, @RequestParam String value, RedirectAttributes ra) {
        specs.add(kind, value);
        ra.addFlashAttribute("success", "Đã thêm giá trị " + kind.label() + ".");
        return "redirect:/staff/warehouse/attributes?kind=" + kind.name();
    }

    @PostMapping("/{id}/rename")
    String rename(@PathVariable long id, @RequestParam SpecKind kind, @RequestParam String value,
            RedirectAttributes ra) {
        specs.rename(id, value);
        ra.addFlashAttribute("success", "Đã đổi tên (các sản phẩm đang dùng được cập nhật theo).");
        return "redirect:/staff/warehouse/attributes?kind=" + kind.name();
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable long id, @RequestParam SpecKind kind, RedirectAttributes ra) {
        specs.delete(id);
        ra.addFlashAttribute("success", "Đã xóa giá trị " + kind.label() + ".");
        return "redirect:/staff/warehouse/attributes?kind=" + kind.name();
    }

    /** So khớp tìm kiếm không phân biệt hoa thường, dấu cách, gạch ("i5 1335" khớp "Core i5-1335U"). */
    private static String compact(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
