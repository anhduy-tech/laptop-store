package com.lapstore.web.sales;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lapstore.common.web.Text;
import com.lapstore.inventory.dto.PosFilterOptions;
import com.lapstore.inventory.dto.PosSearchCriteria;
import com.lapstore.inventory.dto.PosSerialRow;
import com.lapstore.inventory.dto.PosVariantRow;
import com.lapstore.inventory.service.PosStockService;
import com.lapstore.sales.dto.CustomerHit;
import com.lapstore.sales.service.CounterCheckoutService;

/**
 * API JSON chỉ-đọc cho màn bán quầy (ô tìm khách, popup chọn sản phẩm). Ngoại lệ có chủ đích với quy ước "controller trả view",
 * giống StockExcelController. Không có tác dụng phụ, quyền kiểm ở lớp service.
 */
@RestController
@RequestMapping("/staff/sales/counter/api")
class CounterApiController {
    private final CounterCheckoutService counter;
    private final PosStockService stock;

    CounterApiController(CounterCheckoutService counter, PosStockService stock) {
        this.counter = counter;
        this.stock = stock;
    }

    @GetMapping("/customers")
    List<CustomerHit> customers(@RequestParam(defaultValue = "") String q) {
        return counter.searchCustomers(q);
    }

    @GetMapping("/options")
    PosFilterOptions options() {
        return stock.options();
    }

    @GetMapping("/products")
    List<PosVariantRow> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long brandId,
                                 @RequestParam(required = false) String cpu, @RequestParam(required = false) String gpu,
                                 @RequestParam(required = false) Integer ram, @RequestParam(required = false) Integer storage,
                                 @RequestParam(required = false) Long minPrice, @RequestParam(required = false) Long maxPrice,
                                 @RequestParam(defaultValue = "true") boolean onlyAvailable) {
        return stock.search(new PosSearchCriteria(Text.blankToNull(q), brandId, Text.blankToNull(cpu), Text.blankToNull(gpu),
                ram, storage, minPrice, maxPrice, onlyAvailable));
    }

    @GetMapping("/serials")
    List<PosSerialRow> serials(@RequestParam long variantId, @RequestParam(required = false) Long branchId) {
        return stock.serials(variantId, branchId);
    }
}
