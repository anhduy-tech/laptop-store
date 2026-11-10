package com.lapstore.catalog.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.dto.ProductAdminDetail;
import com.lapstore.catalog.dto.ProductAdminRow;
import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.dto.VariantAdminRow;
import com.lapstore.catalog.dto.ProductEditRequest;
import com.lapstore.catalog.dto.ProductEditView;
import com.lapstore.catalog.dto.ReceiptCatalog;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.catalog.repo.ProductAdminSearchRepository;
import com.lapstore.catalog.repo.ProductRepository;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.common.web.Text;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

@Service
class ProductAdminServiceImpl implements ProductAdminService {
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final ProductAdminSearchRepository search;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final PriceHistoryRepository prices;
    private final SpecService specs;

    @PersistenceContext
    private EntityManager em;

    ProductAdminServiceImpl(ProductAdminSearchRepository search, ProductRepository products,
            ProductVariantRepository variants, PriceHistoryRepository prices, SpecService specs) {
        this.search = search;
        this.products = products;
        this.variants = variants;
        this.prices = prices;
        this.specs = specs;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Page<ProductAdminRow> search(String q, String brand, String status, Pageable pageable) {
        return search.search(q, brand, status, pageable).map(r -> new ProductAdminRow(r.variantId(), r.productId(),
                r.productName(), r.brandName(), r.sku(), r.variantName(), r.colorName(), r.price(),
                r.onHand() == null ? 0 : r.onHand().longValue(), r.held() == null ? 0 : r.held().longValue(),
                r.lowStockThreshold() == null ? 0 : r.lowStockThreshold(), r.variantStatus(), r.productStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<String> brandNames() {
        return products.allBrandNames();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public ProductEditView get(long variantId) {
        ProductVariant v = variants.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình sản phẩm " + variantId));
        Product p = v.getProduct();
        if (p.getDeletedAt() != null)
            throw new NotFoundException("Sản phẩm đã bị xóa.");
        return new ProductEditView(v.getId(), p.getId(), p.getBrand().getName(), v.getSku(), v.getColor().getName(),
                v.getCurrentPrice(), p.getName(), p.getSeries(), p.getReleaseYear(), p.getDescription(),
                p.getThumbnailUrl(), p.getStatus(), v.getVariantName(), v.getCpu(), v.getGpu(), v.getRamGb(),
                v.getStorageGb(), v.getWarrantyMonths(), v.getLowStockThreshold(), v.getReturnWindowDays(),
                v.getStatus());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public void update(long variantId, ProductEditRequest r) {
        ProductVariant v = variants.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình sản phẩm " + variantId));
        Product p = v.getProduct();
        if (p.getDeletedAt() != null)
            throw new NotFoundException("Sản phẩm đã bị xóa.");

        String name = Text.orEmpty(r.name());
        String variantName = Text.orEmpty(r.variantName());
        if (name.isEmpty() || variantName.isEmpty())
            throw new BusinessException("Tên sản phẩm và tên cấu hình không được để trống.");
        if (!STATUSES.contains(r.productStatus()) || !STATUSES.contains(r.variantStatus()))
            throw new BusinessException("Trạng thái chỉ nhận ACTIVE hoặc INACTIVE.");
        String thumb = Text.blankToNull(r.thumbnailUrl());
        if (thumb != null && !(thumb.startsWith("http://") || thumb.startsWith("https://") || thumb.startsWith("/")))
            throw new BusinessException("Đường dẫn ảnh phải bắt đầu bằng http://, https:// hoặc /.");
        if (r.ramGb() == null || r.storageGb() == null)
            throw new BusinessException("Chọn RAM và ổ cứng.");
        if (r.warrantyMonths() == null || r.warrantyMonths() < 0)
            throw new BusinessException("Thời hạn bảo hành không được âm.");

        if (!name.equals(p.getName())
                && products.existsByBrand_IdAndNameAndIdNot(p.getBrand().getId(), name, p.getId()))
            throw new BusinessException("Hãng " + p.getBrand().getName() + " đã có sản phẩm tên \"" + name + "\".");
        // CPU/GPU/RAM/ổ cứng chỉ được chọn từ danh mục thuộc tính (thêm mới ở màn Thuộc tính), dùng đúng giá trị đã có
        String cpu = specs.resolve(SpecKind.CPU, r.cpu(), false).value();
        String gpu = Text.blankToNull(r.gpu()) == null ? null : specs.resolve(SpecKind.GPU, r.gpu(), false).value();
        int ram = Integer.parseInt(specs.resolve(SpecKind.RAM, String.valueOf(r.ramGb()), false).value());
        int storage = Integer.parseInt(specs.resolve(SpecKind.STORAGE, String.valueOf(r.storageGb()), false).value());
        for (ProductVariant other : variants.findByProduct_IdAndColor_IdAndCpuAndRamGbAndStorageGb(p.getId(),
                v.getColor().getId(), cpu, ram, storage))
            if (!other.getId().equals(v.getId()) && Objects.equals(gpuKey(other.getGpu()), gpuKey(gpu)))
                throw new BusinessException("Đã có cấu hình khác cùng màu, CPU, GPU, RAM và ổ cứng này.");
        if ("ACTIVE".equals(r.variantStatus()) && !"ACTIVE".equals(v.getStatus())
                && !prices.existsByVariant_IdAndStatus(v.getId(), "APPROVED"))
            throw new BusinessException("Cấu hình này chưa có giá bán được duyệt nên chưa thể mở bán. "
                    + "Admin duyệt giá ở mục Duyệt giá trước.");

        // slug và SKU giữ nguyên: đường dẫn và mã hàng đã được tham chiếu ở nơi khác
        p.setName(name);
        p.setSeries(Text.blankToNull(r.series()));
        p.setReleaseYear(r.releaseYear());
        p.setDescription(Text.blankToNull(r.description()));
        p.setThumbnailUrl(thumb);
        p.setStatus(r.productStatus());

        v.setVariantName(variantName);
        v.setCpu(cpu);
        v.setGpu(gpu);
        v.setRamGb(ram);
        v.setStorageGb(storage);
        v.setWarrantyMonths(r.warrantyMonths());
        if (r.lowStockThreshold() != null)
            v.setLowStockThreshold(r.lowStockThreshold());
        v.setReturnWindowDays(r.returnWindowDays());
        v.setStatus(r.variantStatus());
        try {
            em.flush();
        } catch (PersistenceException e) {
            throw new BusinessException("Cấu hình này trùng với một cấu hình khác của sản phẩm "
                    + "(cùng màu, CPU, GPU, RAM, ổ cứng). Kiểm tra lại.");
        }
    }

    private static String gpuKey(String gpu) {
        return gpu == null ? "" : gpu.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public ReceiptCatalog receiptCatalog() {
        List<IdName> brands = em.createQuery(
                "select new com.lapstore.common.dto.IdName(b.id, b.name) from Brand b where b.active = true order by b.name",
                IdName.class).getResultList();
        List<IdName> categories = em.createQuery(
                "select new com.lapstore.common.dto.IdName(c.id, c.name) from Category c where c.active = true order by c.name",
                IdName.class).getResultList();
        List<ReceiptCatalog.ColorOption> colors = em.createQuery(
                "select new com.lapstore.catalog.dto.ReceiptCatalog$ColorOption(c.id, c.name, c.hexCode) "
                        + "from Color c where c.active = true order by c.name",
                ReceiptCatalog.ColorOption.class).getResultList();
        List<ReceiptCatalog.ProductOption> productOptions = em.createQuery(
                "select new com.lapstore.catalog.dto.ReceiptCatalog$ProductOption(p.id, p.name, b.name) "
                        + "from Product p join p.brand b where p.deletedAt is null order by b.name, p.name",
                ReceiptCatalog.ProductOption.class).getResultList();
        List<ReceiptCatalog.ExistingVariant> existing = em.createQuery(
                "select new com.lapstore.catalog.dto.ReceiptCatalog$ExistingVariant(p.id, v.id, v.sku, c.id, v.cpu, v.gpu, "
                        + "v.ramGb, v.storageGb, v.status, v.currentPrice) "
                        + "from ProductVariant v join v.product p join v.color c where p.deletedAt is null",
                ReceiptCatalog.ExistingVariant.class).getResultList();
        return new ReceiptCatalog(brands, categories, colors, productOptions, existing, specs.choices());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public ProductAdminDetail detail(long productId) {
        Product p = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm " + productId));
        if (p.getDeletedAt() != null)
            throw new NotFoundException("Sản phẩm đã bị xóa.");
        List<VariantAdminRow> rows = search.variantsOf(productId).stream()
                .map(r -> new VariantAdminRow(r.variantId(), r.sku(), r.variantName(), r.colorName(), r.cpu(), r.gpu(),
                        r.ramGb() == null ? 0 : r.ramGb(), r.storageGb() == null ? 0 : r.storageGb(), r.price(),
                        r.onHand() == null ? 0 : r.onHand().longValue(), r.held() == null ? 0 : r.held().longValue(),
                        r.lowStockThreshold() == null ? 0 : r.lowStockThreshold(), r.status()))
                .toList();
        return new ProductAdminDetail(p.getId(), p.getName(), p.getSlug(), p.getBrand().getName(),
                p.getCategory().getName(), p.getSeries(), p.getReleaseYear(), p.getDescription(), p.getThumbnailUrl(),
                p.getStatus(), rows);
    }
}
