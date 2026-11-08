package com.lapstore.catalog.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.dto.ProductCard;
import com.lapstore.catalog.dto.ProductDetail;
import com.lapstore.catalog.dto.VariantOption;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.catalog.repo.ProductRepository;
import com.lapstore.catalog.repo.ProductRow;
import com.lapstore.catalog.repo.ProductSearchRepository;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tra cứu danh mục cho khách (SH-01, SH-02). Chỉ trả sản phẩm và cấu hình đang bán. */
@Service
@Transactional(readOnly = true)
class CatalogServiceImpl implements CatalogService {
    private static final String ACTIVE = "ACTIVE";
    /** Chỉ coi là "đang giảm giá" nếu mức giá cũ vừa hết hiệu lực trong khoảng này. */
    private static final Duration SALE_WINDOW = Duration.ofDays(30);
    private static final Pattern DISCRETE_GPU = Pattern.compile("^(RTX|GTX|RX|Radeon|Arc)\\s?\\w*\\s?\\d{3,4}\\w*");

    private final ProductSearchRepository search;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final PriceHistoryRepository prices;

    CatalogServiceImpl(ProductSearchRepository search, ProductRepository products,
                       ProductVariantRepository variants, PriceHistoryRepository prices) {
        this.search = search;
        this.products = products;
        this.variants = variants;
        this.prices = prices;
    }

    @Override
    public Page<ProductCard> searchProducts(String q, String brand, Pageable pageable) {
        Page<ProductRow> page = search.search(blankToNull(q), blankToNull(brand), pageable);
        List<Long> ids = page.getContent().stream().map(ProductRow::id).toList();
        Map<Long, ProductVariant> cheapest = cheapestVariants(ids);
        Map<Long, BigDecimal> oldPrice = oldPrices(cheapest.values());
        return page.map(r -> toCard(r, cheapest.get(r.id()), oldPrice));
    }

    @Override
    public List<String> brandNames() {
        return products.activeBrandNames();
    }

    @Override
    public ProductDetail getProduct(long productId) {
        Product p = products.findByIdAndStatusAndDeletedAtIsNull(productId, ACTIVE)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm " + productId));
        List<VariantOption> options = variants.findByProduct_IdAndStatusOrderByCurrentPriceAsc(productId, ACTIVE).stream()
                .map(v -> new VariantOption(v.getId(), v.getVariantName(), v.getCurrentPrice().longValue()))
                .toList();
        return new ProductDetail(p.getId(), p.getName(), p.getBrand().getName(), p.getDescription(),
                p.getThumbnailUrl(), options);
    }

    // ---------------- hỗ trợ ----------------

    /** Mỗi sản phẩm lấy cấu hình rẻ nhất đang bán để hiển thị cấu hình tóm tắt. */
    private Map<Long, ProductVariant> cheapestVariants(Collection<Long> productIds) {
        Map<Long, ProductVariant> map = new LinkedHashMap<>();
        if (productIds.isEmpty()) return map;
        for (ProductVariant v : variants.findByProduct_IdInAndStatusOrderByCurrentPriceAsc(productIds, ACTIVE)) {
            map.putIfAbsent(v.getProduct().getId(), v);
        }
        return map;
    }

    /** Giá cũ (cao hơn giá hiện tại) của các cấu hình vừa được giảm giá. */
    private Map<Long, BigDecimal> oldPrices(Collection<ProductVariant> vs) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        if (vs.isEmpty()) return map;
        Instant now = Instant.now();
        List<Long> ids = vs.stream().map(ProductVariant::getId).toList();
        Map<Long, ProductVariant> byId = new LinkedHashMap<>();
        vs.forEach(v -> byId.put(v.getId(), v));
        for (PriceHistory ph : prices.findRecentlyEnded(ids, now, now.minus(SALE_WINDOW))) {
            Long vid = ph.getVariant().getId();
            BigDecimal current = byId.get(vid).getCurrentPrice();
            if (!map.containsKey(vid) && ph.getPrice().compareTo(current) > 0) map.put(vid, ph.getPrice());
        }
        return map;
    }

    private static ProductCard toCard(ProductRow r, ProductVariant v, Map<Long, BigDecimal> oldPrice) {
        long price = r.minPrice() == null ? 0L : r.minPrice().longValue();
        Long old = null;
        String sale = null;
        if (v != null && v.getCurrentPrice().compareTo(r.minPrice()) == 0) {
            BigDecimal o = oldPrice.get(v.getId());
            if (o != null) {
                old = o.longValue();
                long pct = o.subtract(r.minPrice()).multiply(BigDecimal.valueOf(100)).divide(o, 0, RoundingMode.HALF_UP).longValue();
                if (pct > 0) sale = "-" + pct + "%";
            }
        }
        return new ProductCard(r.id(), r.name(), r.brand(), price, old, sale,
                v == null ? List.of() : specs(v), r.thumbnailUrl());
    }

    /** Chip cấu hình ngắn gọn: CPU, RAM, ổ cứng, và card rời nếu có. */
    static List<String> specs(ProductVariant v) {
        List<String> out = new ArrayList<>();
        out.add(shortCpu(v.getCpu()));
        out.add(v.getRamGb() + "GB");
        int gb = v.getStorageGb();
        out.add(gb >= 1024 && gb % 1024 == 0 ? (gb / 1024) + "TB" : gb + "GB");
        String gpu = v.getGpu();
        if (gpu != null) {
            Matcher m = DISCRETE_GPU.matcher(gpu);
            if (m.find()) out.add(m.group().trim());
        }
        return out;
    }

    private static String shortCpu(String cpu) {
        if (cpu == null) return "";
        return cpu.replace("Intel Core ", "").replace("Intel ", "").replace("Apple ", "").trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
