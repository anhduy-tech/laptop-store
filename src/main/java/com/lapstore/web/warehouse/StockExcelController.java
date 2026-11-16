package com.lapstore.web.warehouse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.Text;
import com.lapstore.inventory.service.InventoryExcelService;
import com.lapstore.inventory.service.StockQueryService;
import com.lapstore.inventory.service.StocktakeService;

/**
 * Xuất/nhập Excel cho kho. Ngoại lệ với quy ước "controller trả tên view": các hàm này trả file hoặc văn bản thuần
 * (POST /excel/serials được JS dùng bằng fetch để điền serial vào ô nhập, lỗi trả HTTP 400 kèm thông báo tiếng Việt).
 */
@Controller
@RequestMapping("/staff/warehouse")
class StockExcelController {
    private static final MediaType XLSX = MediaType
            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final int EXPORT_LIMIT = 50_000;
    private static final java.util.Set<String> SERIAL_STATUSES = java.util.Set.of("IN_STOCK", "IN_TRANSIT", "SOLD",
            "WARRANTY", "RETURNED", "DEFECTIVE");

    private final InventoryExcelService excel;
    private final StockQueryService stock;
    private final StocktakeService stocktakes;

    StockExcelController(InventoryExcelService excel, StockQueryService stock, StocktakeService stocktakes) {
        this.excel = excel;
        this.stock = stock;
        this.stocktakes = stocktakes;
    }

    @GetMapping("/excel/serial-template.xlsx")
    ResponseEntity<byte[]> template() {
        return file(excel.serialTemplate(), "mau-danh-sach-serial.xlsx");
    }

    /** Đọc file Excel, trả mỗi serial một dòng (văn bản thuần) để điền vào ô serial. */
    @PostMapping(value = "/excel/serials", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    ResponseEntity<String> parse(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body("File rỗng.");
        try {
            List<String> serials = excel.readSerials(file.getInputStream());
            return ResponseEntity.ok(String.join("\n", serials));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Không đọc được file.");
        }
    }

    @GetMapping("/stock/export.xlsx")
    ResponseEntity<byte[]> exportStock(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId, @RequestParam(defaultValue = "false") boolean low) {
        var rows = stock.search(Text.blankToNull(q), branchId, low, PageRequest.of(0, EXPORT_LIMIT)).getContent();
        return file(excel.exportStock(rows), "ton-kho.xlsx");
    }

    @GetMapping("/stock/{variantId}/{branchId}/serials.xlsx")
    ResponseEntity<byte[]> exportSerials(@PathVariable long variantId, @PathVariable long branchId,
            @RequestParam(required = false) String status) {
        String st = Text.blankToNull(status);
        if (st != null && !SERIAL_STATUSES.contains(st))
            st = null;
        var view = stock.serials(variantId, branchId, st, EXPORT_LIMIT);
        boolean cost = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> "COST_VIEW".equals(a.getAuthority()));
        return file(excel.exportSerials(view, cost), "serial-" + view.sku() + ".xlsx");
    }

    @GetMapping("/stocktakes/{code}/export.xlsx")
    ResponseEntity<byte[]> exportStocktake(@PathVariable String code) {
        var header = stocktakes.get(code, null, PageRequest.of(0, 1)).header();
        return file(excel.exportStocktake(header, stocktakes.allItems(code)), "kiem-ke-" + code + ".xlsx");
    }

    private static ResponseEntity<byte[]> file(byte[] data, String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .contentType(XLSX).body(data);
    }
}
