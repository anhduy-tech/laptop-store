package com.lapstore.web.shop;

import com.lapstore.catalog.dto.ProductDetail;
import com.lapstore.catalog.service.CatalogService;
import com.lapstore.common.web.Text;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** SH-01 danh sách laptop · SH-02 chi tiết sản phẩm. */
@Controller
@RequestMapping("/shop")
class ShopCatalogController {
    private static final int PAGE_SIZE = 12;

    private final CatalogService catalog;

    ShopCatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping({"", "/"})
    String index(@RequestParam(required = false) String q,
                 @RequestParam(required = false) String brand,
                 @RequestParam(name = "page", defaultValue = "0") int pageNo,
                 Model model) {
        String keyword = Text.orEmpty(q);
        String selected = Text.orEmpty(brand);
        model.addAttribute("page", catalog.searchProducts(keyword, selected, PageRequest.of(Math.max(pageNo, 0), PAGE_SIZE)));
        model.addAttribute("brands", catalog.brandNames());
        model.addAttribute("q", keyword);
        model.addAttribute("selectedBrand", selected);
        return "shop/index";
    }

    @GetMapping("/products/{id}")
    String detail(@PathVariable long id, Model model) {
        ProductDetail product = catalog.getProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("variants", product.variants());
        return "shop/product-detail";
    }
}
