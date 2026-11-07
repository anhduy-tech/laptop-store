package com.lapstore.web.preview;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Xem trước 26 màn hình bằng DỮ LIỆU GIẢ — chỉ bật khi chạy profile "ui".
 * mvnw spring-boot:run -Dspring-boot.run.profiles=ui
 * -> http://localhost:8080/shop/_ui (thêm ?empty=1 để xem empty state)
 * Không dùng cho production. Khi có controller thật, xóa package này.
 */
@Controller
@Profile("ui")
@RequestMapping("/shop/_ui")
class UiPreviewController {

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2)
            r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    /** slug, mã, tên, view, nav */
    private static final String[][] SCREENS = {
            { "sh-01", "SH-01", "Danh sách laptop", "shop/index", "Shop" },
            { "sh-02", "SH-02", "Chi tiết sản phẩm", "shop/product-detail", "Shop" },
            { "sh-03", "SH-03", "Giỏ hàng", "shop/cart", "Shop" },
            { "sh-04", "SH-04", "Checkout", "shop/checkout", "Shop" },
            { "sh-05", "SH-05", "Tra cứu đơn", "shop/order-lookup", "Shop" },
            { "sh-06", "SH-06", "Đặt hàng thành công", "shop/order-success", "Shop" },
            { "sh-07", "SH-07", "Yêu cầu đổi trả", "aftersales/return-form", "Shop" },
            { "au-01", "AU-01", "Đăng nhập NV", "login", "Auth" },
            { "sa-01", "SA-01", "Bán quầy", "sales/counter", "Sales" },
            { "sa-02", "SA-02", "Danh sách đơn", "sales/order-list", "Sales" },
            { "sa-03", "SA-03", "Chi tiết đơn / gán serial", "sales/order-detail", "Sales" },
            { "co-01", "CO-01", "Danh sách chat", "consult/index", "Consult" },
            { "co-02", "CO-02", "Khung chat", "consult/chat", "Consult" },
            { "wh-00", "WH-00", "Menu kho & KM", "warehouse/index", "Warehouse" },
            { "wh-01", "WH-01", "Nhập kho", "warehouse/stock-receipt", "Warehouse" },
            { "wh-02", "WH-02", "Chuyển kho", "warehouse/transfer", "Warehouse" },
            { "wh-03", "WH-03", "Campaign & voucher", "warehouse/campaigns", "Warehouse" },
            { "wh-04", "WH-04", "Đề xuất giá", "warehouse/price-propose", "Warehouse" },
            { "ad-00", "AD-00", "Dashboard admin", "admin/index", "Admin" },
            { "ad-01", "AD-01", "Quản lý user", "admin/users", "Admin" },
            { "ad-02", "AD-02", "Xếp ca", "admin/shifts", "Admin" },
            { "ad-03", "AD-03", "Duyệt giá", "admin/price-approve", "Admin" },
            { "ad-04", "AD-04", "Báo cáo doanh thu", "admin/reports", "Admin" },
            { "er-403", "ER-403", "Không có quyền", "error/403", "Lỗi" },
            { "er-404", "ER-404", "Không tìm thấy", "error/404", "Lỗi" },
            { "er-5xx", "ER-5xx", "Lỗi máy chủ", "error/5xx", "Lỗi" },
    };

    @GetMapping
    String index(Model model) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] s : SCREENS)
            list.add(m("slug", s[0], "code", s[1], "name", s[2], "group", s[4]));
        model.addAttribute("screens", list);
        return "preview/index";
    }

    @GetMapping("/{slug}")
    String show(@PathVariable String slug, @RequestParam(defaultValue = "0") int empty, Model model) {
        boolean e = empty == 1;
        String view = null;
        for (String[] s : SCREENS)
            if (s[0].equals(slug))
                view = s[3];
        if (view == null)
            return "error/404";

        model.addAttribute("preview", true);
        model.addAttribute("cartCount", 2);
        model.addAttribute("loginUrl", "/staff/login");
        String nav = view.startsWith("sales") ? "sales"
                : view.startsWith("consult") ? "consult"
                        : view.startsWith("warehouse") ? "warehouse" : view.startsWith("admin") ? "admin" : "";
        model.addAttribute("nav", nav);

        List<Map<String, Object>> cartItems = e ? List.of()
                : List.of(
                        m("id", 1, "variantName", "ASUS ROG Strix G16 · i7 / 16GB / 1TB / RTX 4060", "unitPrice",
                                38990000L, "quantity", 1),
                        m("id", 2, "variantName", "Dell XPS 13 · Ultra 7 / 16GB / 512GB", "unitPrice", 32500000L,
                                "quantity", 2));
        long total = e ? 0 : 38990000L + 2 * 32500000L;

        switch (slug) {
            case "sh-01" -> {
                List<Map<String, Object>> cards = e ? List.of()
                        : List.of(
                                m("id", 1, "name", "ASUS ROG Strix G16 (2025)", "brand", "ASUS", "price", 38990000L,
                                        "oldPrice", 42990000L, "sale", "-9%", "specs",
                                        List.of("i7", "16GB", "1TB", "RTX 4060"), "thumbnail", null),
                                m("id", 2, "name", "MacBook Air 13 M3", "brand", "Apple", "price", 27990000L,
                                        "oldPrice", 29990000L, "sale", "-7%", "specs", List.of("M3", "8GB", "256GB"),
                                        "thumbnail", null),
                                m("id", 3, "name", "Dell XPS 13 Plus", "brand", "Dell", "price", 32500000L, "oldPrice",
                                        34990000L, "sale", "-7%", "specs", List.of("Ultra 7", "16GB", "OLED"),
                                        "thumbnail", null),
                                m("id", 4, "name", "Lenovo ThinkPad X1 Carbon Gen 12", "brand", "Lenovo", "price",
                                        44900000L, "oldPrice", 47900000L, "sale", "-6%", "specs",
                                        List.of("Ultra 7", "32GB", "1.1kg"), "thumbnail", null),
                                m("id", 5, "name", "HP Spectre x360 14", "brand", "HP", "price", 36900000L, "oldPrice",
                                        39900000L, "sale", "-8%", "specs", List.of("Ultra 7", "16GB", "2-in-1"),
                                        "thumbnail", null),
                                m("id", 6, "name", "Acer Swift Go 14", "brand", "Acer", "price", 18490000L, "oldPrice",
                                        20990000L, "sale", "-12%", "specs", List.of("i5", "16GB", "OLED"), "thumbnail",
                                        null));
                model.addAttribute("page", m("content", cards, "totalPages", e ? 0 : 3, "number", 0));
                model.addAttribute("brands", List.of("ASUS", "Apple", "Dell", "Lenovo", "HP", "Acer"));
                model.addAttribute("q", "");
                model.addAttribute("selectedBrand", "");
            }
            case "sh-02" -> {
                model.addAttribute("product", m("name", "ASUS ROG Strix G16 (2025)", "brand", "ASUS", "thumbnail", null,
                        "description",
                        "Laptop gaming mạnh mẽ với màn hình 16 inch 165Hz, tản nhiệt kép và bàn phím RGB. Phù hợp chơi game, đồ họa và làm việc nặng."));
                model.addAttribute("variants", e ? List.of()
                        : List.of(
                                m("id", 11, "variantName", "i7 / 16GB / 512GB / RTX 4050", "currentPrice", 33990000L),
                                m("id", 12, "variantName", "i7 / 16GB / 1TB / RTX 4060", "currentPrice", 38990000L),
                                m("id", 13, "variantName", "i9 / 32GB / 1TB / RTX 4070", "currentPrice", 52990000L)));
            }
            case "sh-03" -> {
                model.addAttribute("items", cartItems);
                model.addAttribute("total", total);
            }
            case "sh-04" -> {
                model.addAttribute("items", cartItems);
                model.addAttribute("total", total);
                model.addAttribute("form", m("fullName", "", "phone", "", "email", "", "receiveMethod", "PICKUP",
                        "address", "", "voucherCode", ""));
                model.addAttribute("errors", m());
            }
            case "sh-05" -> {
                model.addAttribute("code", "LS2611-0001");
                model.addAttribute("phone", "0901234567");
                if (e)
                    model.addAttribute("notFound", true);
                else
                    model.addAttribute("order",
                            m("code", "LS2611-0001", "status", "CONFIRMED", "total", total, "items", cartItems));
            }
            case "sh-06" -> model.addAttribute("orderCode", "LS2611-0001");
            case "sh-07" -> {
                model.addAttribute("form",
                        m("orderCode", "", "phone", "", "type", "RETURN", "serial", "", "reason", ""));
                model.addAttribute("errors", m());
            }
            case "sa-01" -> {
                if (!e)
                    model.addAttribute("order", m("code", "CT-0042", "total", 66490000L, "lines", List.of(
                            m("serial", "SN-A10031", "variantName", "ASUS ROG Strix G16 · 1TB", "price", 38990000L),
                            m("serial", "SN-B20077", "variantName", "Acer Swift Go 14 · 512GB", "price", 27500000L))));
            }
            case "sa-02" -> {
                model.addAttribute("statuses", List.of("NEW", "CONFIRMED", "COMPLETED", "CANCELLED"));
                model.addAttribute("status", "");
                model.addAttribute("q", "");
                model.addAttribute("orders", e ? List.of()
                        : List.of(
                                m("id", 1, "code", "LS2611-0001", "channel", "ONLINE", "customerName", "Nguyễn Văn An",
                                        "total", 38990000L, "status", "NEW"),
                                m("id", 2, "code", "CT-0042", "channel", "COUNTER", "customerName", "Trần Thị Bình",
                                        "total", 27990000L, "status", "COMPLETED"),
                                m("id", 3, "code", "LS2611-0003", "channel", "ONLINE", "customerName", "Lê Minh Châu",
                                        "total", 65000000L, "status", "CONFIRMED"),
                                m("id", 4, "code", "LS2611-0004", "channel", "ONLINE", "customerName", "Phạm Quốc Dũng",
                                        "total", 18490000L, "status", "CANCELLED")));
            }
            case "sa-03" -> model.addAttribute("order", m("id", 1, "code", "LS2611-0001", "status", "CONFIRMED",
                    "customerName", "Nguyễn Văn An", "phone", "0901234567", "address",
                    "12 Nguyễn Trãi, Hà Đông, Hà Nội", "total", total,
                    "items", List.of(
                            m("id", 1, "variantName", "ASUS ROG Strix G16 · 1TB", "unitPrice", 38990000L, "quantity", 1,
                                    "serials", List.of("SN-A10031")),
                            m("id", 2, "variantName", "Dell XPS 13 · 512GB", "unitPrice", 32500000L, "quantity", 2,
                                    "serials", List.of()))));
            case "co-01" -> model.addAttribute("sessions", e ? List.of()
                    : List.of(
                            m("id", 1, "customerName", "Khách Hoàng", "mode", "HUMAN", "status", "OPEN", "lastAt",
                                    "10:42"),
                            m("id", 2, "customerName", "Khách Mai", "mode", "AI", "status", "WAITING", "lastAt",
                                    "10:15"),
                            m("id", 3, "customerName", "Khách Tùng", "mode", "HUMAN", "status", "CLOSED", "lastAt",
                                    "Hôm qua")));
            case "co-02" -> {
                model.addAttribute("chat", m("id", 1, "customerName", "Khách Hoàng"));
                model.addAttribute("messages", e ? List.of()
                        : List.of(
                                m("sender", "GUEST", "content",
                                        "Chào shop, mình cần laptop học lập trình tầm 20 triệu.", "time", "10:40"),
                                m("sender", "AI", "content", "Xin chào! Bạn ưu tiên pin lâu hay hiệu năng cao ạ?",
                                        "time", "10:40"),
                                m("sender", "STAFF", "content",
                                        "Mình là Lan bên tư vấn. Acer Swift Go 14 đang rất hợp ngân sách của bạn.",
                                        "time", "10:42")));
            }
            case "wh-01" -> {
                model.addAttribute("suppliers",
                        List.of(m("id", 1, "name", "FPT Trading"), m("id", 2, "name", "Synnex")));
                model.addAttribute("branches",
                        List.of(m("id", 1, "name", "CN Hà Đông"), m("id", 2, "name", "CN Cầu Giấy")));
            }
            case "wh-02" -> model.addAttribute("branches",
                    List.of(m("id", 1, "name", "CN Hà Đông"), m("id", 2, "name", "CN Cầu Giấy")));
            case "wh-03" -> {
                model.addAttribute("campaigns", e ? List.of()
                        : List.of(
                                m("name", "Back to School", "type", "PERCENT", "period", "01/11 – 30/11", "status",
                                        "ACTIVE"),
                                m("name", "Black Friday", "type", "AMOUNT", "period", "24/11 – 30/11", "status",
                                        "DRAFT")));
                model.addAttribute("vouchers", e ? List.of()
                        : List.of(
                                m("code", "LAP10", "used", 24, "reserved", 3, "limit", 100, "status", "ACTIVE"),
                                m("code", "NEWUSER", "used", 100, "reserved", 0, "limit", 100, "status", "CLOSED")));
            }
            case "ad-00" -> {
                model.addAttribute("pendingPrices", 3);
                model.addAttribute("kpi", m("revenue", 121000000L, "orders", 4, "onDuty", 5));
            }
            case "ad-01" -> model.addAttribute("users", e ? List.of()
                    : List.of(
                            m("id", 1, "username", "admin", "fullName", "Quản trị viên", "email", "admin@lapstore.vn",
                                    "branchName", "Kho tổng Long Biên", "roles", List.of("ADMIN"), "active", true,
                                    "locked", false),
                            m("id", 2, "username", "sales01", "fullName", "Nguyễn Thị Lan", "email",
                                    "lan.nguyen@lapstore.vn", "branchName", "Lapstore Hà Đông", "roles",
                                    List.of("SALES"), "active", true, "locked", true),
                            m("id", 3, "username", "kho01", "fullName", "Lê Văn Nam", "email", "nam.le@lapstore.vn",
                                    "branchName", "Kho tổng Long Biên", "roles", List.of("WAREHOUSE_PROMO"), "active",
                                    false, "locked", false)));
            case "ad-02" -> {
                model.addAttribute("users", List.of(m("id", 2, "name", "Nguyễn Thị Lan")));
                model.addAttribute("templates",
                        List.of(m("id", 1, "name", "Ca sáng (08–13)"), m("id", 2, "name", "Ca chiều (13–18)")));
                model.addAttribute("branches", List.of(m("id", 1, "name", "CN Hà Đông")));
                model.addAttribute("assignments", e ? List.of()
                        : List.of(
                                m("date", "05/11/2026", "userName", "Nguyễn Thị Lan", "shift", "Ca sáng", "branch",
                                        "CN Hà Đông"),
                                m("date", "06/11/2026", "userName", "Trần Văn Nam", "shift", "Ca chiều", "branch",
                                        "CN Cầu Giấy")));
            }
            case "ad-03" -> model.addAttribute("proposals", e ? List.of()
                    : List.of(
                            m("id", 1, "sku", "ROG-G16-1TB", "newPrice", 36990000L, "effectiveFrom", "10/11/2026",
                                    "reason", "Cạnh tranh giá với đối thủ"),
                            m("id", 2, "sku", "MBA-M3-256", "newPrice", 26490000L, "effectiveFrom", "15/11/2026",
                                    "reason", "Chương trình Back to School")));
            case "ad-04" -> {
                model.addAttribute("from", "2026-11-01");
                model.addAttribute("to", "2026-11-05");
                model.addAttribute("branches", List.of(m("id", 1, "name", "CN Hà Đông")));
                model.addAttribute("rows", e ? List.of()
                        : List.of(
                                m("date", "04/11", "branch", "CN Hà Đông", "orders", 6, "revenue", 182000000L, "profit",
                                        21500000L),
                                m("date", "05/11", "branch", "CN Hà Đông", "orders", 4, "revenue", 121000000L, "profit",
                                        14200000L)));
                model.addAttribute("totalOrders", e ? 0 : 10);
                model.addAttribute("totalRevenue", 303000000L);
                model.addAttribute("totalProfit", 35700000L);
            }
            default -> {
            }
        }
        if (slug.equals("sh-01") || slug.equals("sa-02"))
            model.addAttribute("success", "Đây là thông báo flash mẫu.");
        return view;
    }
}
