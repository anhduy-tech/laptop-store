package com.lapstore.web.warehouse;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lapstore.catalog.dto.NewProductSpec;
import com.lapstore.catalog.service.ProductAdminService;
import com.lapstore.common.web.FormErrors;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.ReceiptResult;
import com.lapstore.inventory.dto.StockReceiptRequest;
import com.lapstore.inventory.dto.StockReceiptRequest.ProductLine;
import com.lapstore.inventory.dto.StockReceiptRequest.VariantLine;
import com.lapstore.inventory.service.InventoryService;

/**
 * WH-01 nhập kho: chọn hoặc tạo sản phẩm, sinh biến thể theo tổ hợp thuộc tính, mỗi biến thể có giá và serial.
 * Giao diện do receipt.js dựng; khi gửi, nó sinh các trường products[i].variants[j].* để Spring bind.
 */
@Controller
@RequestMapping("/staff/warehouse/receipts")
class StockReceiptController {
    private final InventoryService inventory;
    private final ProductAdminService products;

    StockReceiptController(InventoryService inventory, ProductAdminService products) {
        this.inventory = inventory;
        this.products = products;
    }

    /** Một sản phẩm x vài chục biến thể x nhiều trường: nới giới hạn tự tăng danh sách (mặc định 256). */
    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(1000);
    }

    @GetMapping
    String form(Model model) {
        model.addAttribute("suppliers", inventory.suppliers());
        model.addAttribute("branches", inventory.branches());
        model.addAttribute("catalog", products.receiptCatalog());
        return "warehouse/stock-receipt";
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") StockReceiptForm form, BindingResult br, Principal principal,
            RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", FormErrors.first(br));
            return "redirect:/staff/warehouse/receipts";
        }
        ReceiptResult res = inventory.createReceipt(toRequest(form), principal.getName());
        StringBuilder msg = new StringBuilder("Đã nhập kho phiếu ").append(res.code()).append(": ")
                .append(res.serialCount()).append(" máy.");
        if (res.newProducts() > 0)
            msg.append(" Tạo ").append(res.newProducts()).append(" sản phẩm mới.");
        if (res.newVariants() > 0)
            msg.append(" Tạo ").append(res.newVariants())
                    .append(" cấu hình mới, đang chờ admin duyệt giá bán rồi mới mở bán.");
        ra.addFlashAttribute("success", msg.toString());
        return "redirect:/staff/warehouse/receipts";
    }

    private static StockReceiptRequest toRequest(StockReceiptForm f) {
        List<ProductLine> lines = f.getProducts().stream().filter(Objects::nonNull).map(b -> {
            NewProductSpec np = b.getProductId() != null ? null
                    : new NewProductSpec(b.getBrandId(), Text.blankToNull(b.getNewBrandName()), b.getCategoryId(),
                            b.getName(), b.getSeries(), b.getReleaseYear(), b.getDescription());
            List<VariantLine> rows = b.getVariants().stream().filter(Objects::nonNull)
                    .map(r -> new VariantLine(r.getColorId(), r.getCpu(), r.getGpu(), r.getRamGb(), r.getStorageGb(),
                            r.getUnitCost(), r.getSellPrice(), splitLines(r.getSerials())))
                    .toList();
            return new ProductLine(b.getProductId(), np, rows);
        }).toList();
        return new StockReceiptRequest(f.getSupplierId(), f.getBranchId(), Text.blankToNull(f.getNote()), lines);
    }

    private static List<String> splitLines(String text) {
        return text == null ? List.of() : Arrays.stream(text.split("\\R")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList();
    }
}
