package com.lapstore.web.warehouse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.service.SpecService;

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
    String list(@RequestParam(defaultValue = "CPU") SpecKind kind, Model model) {
        model.addAttribute("kinds", SpecKind.values());
        model.addAttribute("kind", kind);
        model.addAttribute("rows", specs.list(kind));
        return "warehouse/attributes";
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
}
