package com.lapstore.web.warehouse;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.catalog.dto.ProductEditRequest;
import com.lapstore.catalog.dto.ProductEditView;
import com.lapstore.catalog.service.ProductAdminService;
import com.lapstore.catalog.service.SpecService;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;

import jakarta.validation.Valid;

/** WH-05 bảng sản phẩm (xem) và sửa thông tin sản phẩm. Quyền sửa PRODUCT_MANAGE được kiểm ở service. */
@Controller
@RequestMapping("/staff/warehouse/products")
class ProductAdminController {
    private static final int PAGE_SIZE = 20;

    private final ProductAdminService products;
    private final SpecService specs;

    ProductAdminController(ProductAdminService products, SpecService specs) {
        this.products = products;
        this.specs = specs;
    }

    @GetMapping
    String list(@RequestParam(required = false) String q, @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, Model model) {
        String qq = Text.blankToNull(q);
        String bb = Text.blankToNull(brand);
        String ss = Text.blankToNull(status);
        if (ss != null && !ss.equals("ACTIVE") && !ss.equals("INACTIVE"))
            ss = null;
        model.addAttribute("page", products.search(qq, bb, ss, PageRequest.of(Math.max(page, 0), PAGE_SIZE)));
        model.addAttribute("brands", products.brandNames());
        model.addAttribute("q", qq);
        model.addAttribute("selectedBrand", bb);
        model.addAttribute("selectedStatus", ss);
        return "warehouse/products";
    }

    @GetMapping("/{productId}")
    String detail(@PathVariable long productId, Model model) {
        model.addAttribute("product", products.detail(productId));
        return "warehouse/product-detail";
    }

    @GetMapping("/{variantId}/edit")
    String editForm(@PathVariable long variantId, Model model) {
        ProductEditView item = products.get(variantId);
        if (!model.containsAttribute("form"))
            model.addAttribute("form", toForm(item));
        model.addAttribute("item", item);
        model.addAttribute("specs", specs.choices());
        return "warehouse/product-edit";
    }

    @PostMapping("/{variantId}/edit")
    String update(@PathVariable long variantId, @Valid @ModelAttribute("form") ProductEditForm form,
            BindingResult br, RedirectAttributes ra) {
        String path = "/staff/warehouse/products/" + variantId + "/edit";
        if (br.hasErrors())
            return backToForm(form, FormErrors.of(br), null, path, ra);
        try {
            products.update(variantId, new ProductEditRequest(form.getName(), form.getSeries(), form.getReleaseYear(),
                    form.getDescription(), form.getThumbnailUrl(), form.getProductStatus(), form.getVariantName(),
                    form.getCpu(), form.getGpu(), form.getRamGb(), form.getStorageGb(), form.getWarrantyMonths(),
                    form.getLowStockThreshold(), form.getReturnWindowDays(), form.getVariantStatus()));
        } catch (BusinessException e) {
            return backToForm(form, null, e.getMessage(), path, ra);
        }
        ra.addFlashAttribute("success", "Đã cập nhật sản phẩm.");
        return "redirect:/staff/warehouse/products";
    }

    private static String backToForm(ProductEditForm form, java.util.Map<String, String> errors, String error,
            String path, RedirectAttributes ra) {
        ra.addFlashAttribute("form", form);
        if (errors != null)
            ra.addFlashAttribute("errors", errors);
        if (error != null)
            ra.addFlashAttribute("error", error);
        return "redirect:" + path;
    }

    private static ProductEditForm toForm(ProductEditView v) {
        ProductEditForm f = new ProductEditForm();
        f.setName(v.name());
        f.setSeries(v.series());
        f.setReleaseYear(v.releaseYear());
        f.setDescription(v.description());
        f.setThumbnailUrl(v.thumbnailUrl());
        f.setProductStatus(v.productStatus());
        f.setVariantName(v.variantName());
        f.setCpu(v.cpu());
        f.setGpu(v.gpu());
        f.setRamGb(v.ramGb());
        f.setStorageGb(v.storageGb());
        f.setWarrantyMonths(v.warrantyMonths());
        f.setLowStockThreshold(v.lowStockThreshold());
        f.setReturnWindowDays(v.returnWindowDays());
        f.setVariantStatus(v.variantStatus());
        return f;
    }
}
