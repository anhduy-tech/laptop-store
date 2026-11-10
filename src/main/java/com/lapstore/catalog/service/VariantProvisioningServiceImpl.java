package com.lapstore.catalog.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.Brand;
import com.lapstore.catalog.domain.Category;
import com.lapstore.catalog.domain.Color;
import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.dto.NewProductSpec;
import com.lapstore.catalog.dto.ResolvedVariant;
import com.lapstore.catalog.dto.SpecKind;
import com.lapstore.catalog.dto.VariantSpec;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.catalog.repo.ProductRepository;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.Text;
import com.lapstore.staff.domain.AppUser;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
class VariantProvisioningServiceImpl implements VariantProvisioningService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final PriceHistoryRepository prices;
    private final SpecService specs;

    @PersistenceContext
    private EntityManager em;

    VariantProvisioningServiceImpl(ProductRepository products, ProductVariantRepository variants,
            PriceHistoryRepository prices, SpecService specs) {
        this.products = products;
        this.variants = variants;
        this.prices = prices;
        this.specs = specs;
    }

    @Override
    @Transactional
    public long createProduct(NewProductSpec s) {
        requireManage();
        String name = Text.orEmpty(s.name());
        if (name.isEmpty() || name.length() > 200)
            throw new BusinessException("Tên sản phẩm mới không được để trống và tối đa 200 ký tự.");
        Category category = s.categoryId() == null ? null : em.find(Category.class, s.categoryId());
        if (category == null || !category.isActive())
            throw new BusinessException("Chọn danh mục cho sản phẩm \"" + name + "\".");
        Brand brand = resolveBrand(s, name);
        if (brand.getId() != null && products.existsByBrand_IdAndName(brand.getId(), name))
            throw new BusinessException("Hãng " + brand.getName() + " đã có sản phẩm \"" + name
                    + "\". Hãy chọn sản phẩm đó thay vì tạo mới.");
        if (s.releaseYear() != null && (s.releaseYear() < 1990 || s.releaseYear() > 2100))
            throw new BusinessException("Năm ra mắt của \"" + name + "\" không hợp lệ.");

        Product p = new Product();
        p.setBrand(brand);
        p.setCategory(category);
        p.setName(name);
        p.setSlug(uniqueSlug(name));
        p.setSeries(Text.blankToNull(s.series()));
        p.setReleaseYear(s.releaseYear());
        p.setDescription(Text.blankToNull(s.description()));
        p.setStatus("ACTIVE");
        try {
            products.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Không tạo được sản phẩm \"" + name + "\" (trùng tên hoặc đường dẫn). Thử lại.");
        }
        return p.getId();
    }

    @Override
    @Transactional
    public ResolvedVariant findOrCreateVariant(VariantSpec s, String priceReason, String username) {
        Product p = s.productId() == null ? null : products.findById(s.productId()).orElse(null);
        if (p == null || p.getDeletedAt() != null)
            throw new BusinessException("Sản phẩm không tồn tại hoặc đã bị xóa.");
        Color color = s.colorId() == null ? null : em.find(Color.class, s.colorId());
        if (color == null || !color.isActive())
            throw new BusinessException("Màu không hợp lệ cho \"" + p.getName() + "\".");
        if (s.ramGb() == null || s.storageGb() == null)
            throw new BusinessException("Chọn RAM và ổ cứng.");

        // Thuộc tính lấy theo danh mục: trùng (không phân biệt hoa thường/dấu/khoảng trắng) thì dùng giá trị đã có;
        // chưa có thì tạo mới (cần PRODUCT_MANAGE). GPU không bắt buộc: trống = máy không có GPU rời.
        String cpu = specs.resolve(SpecKind.CPU, s.cpu(), true).value();
        String gpuRaw = Text.blankToNull(s.gpu());
        String gpu = gpuRaw == null ? null : specs.resolve(SpecKind.GPU, gpuRaw, true).value();
        int ram = Integer.parseInt(specs.resolve(SpecKind.RAM, String.valueOf(s.ramGb()), true).value());
        int storage = Integer.parseInt(specs.resolve(SpecKind.STORAGE, String.valueOf(s.storageGb()), true).value());

        List<ProductVariant> same = variants.findByProduct_IdAndColor_IdAndCpuAndRamGbAndStorageGb(p.getId(),
                color.getId(), cpu, ram, storage);
        for (ProductVariant v : same)
            if (gpuKey(v.getGpu()).equals(gpuKey(gpu)))
                return new ResolvedVariant(v.getId(), v.getSku(), false);

        requireManage();
        if (s.sellPrice() == null || s.sellPrice().signum() <= 0)
            throw new BusinessException("Cấu hình mới " + describe(p, color, cpu, gpu, ram, storage)
                    + " cần giá bán lớn hơn 0.");
        AppUser user = user(username);

        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setColor(color);
        v.setCpu(cpu);
        v.setGpu(gpu);
        v.setRamGb(ram);
        v.setStorageGb(storage);
        v.setSku(uniqueSku(p, color, gpu, ram, storage));
        v.setVariantName(variantName(p, color, cpu, gpu, ram, storage));
        v.setCurrentPrice(s.sellPrice()); // chưa dùng để bán: biến thể INACTIVE tới khi admin duyệt giá
        v.setWarrantyMonths(12);
        v.setLowStockThreshold(3);
        v.setStatus("INACTIVE");
        try {
            variants.saveAndFlush(v);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Cấu hình " + describe(p, color, cpu, gpu, ram, storage)
                    + " vừa được tạo ở nơi khác. Thử lại.");
        }

        PriceHistory ph = new PriceHistory();
        ph.setVariant(v);
        ph.setPrice(s.sellPrice());
        ph.setEffectiveFrom(Instant.now());
        ph.setStatus("PENDING");
        ph.setReason(priceReason);
        ph.setCreatedBy(user);
        prices.save(ph);
        return new ResolvedVariant(v.getId(), v.getSku(), true);
    }

    // ------------------------------------------------------------ tiện ích

    private Brand resolveBrand(NewProductSpec s, String productName) {
        if (s.brandId() != null) {
            Brand b = em.find(Brand.class, s.brandId());
            if (b == null || !b.isActive())
                throw new BusinessException("Hãng của \"" + productName + "\" không hợp lệ.");
            return b;
        }
        String bn = Text.orEmpty(s.newBrandName());
        if (bn.isEmpty() || bn.length() > 80)
            throw new BusinessException("Chọn hãng hoặc nhập tên hãng mới cho \"" + productName + "\" (tối đa 80 ký tự).");
        List<Brand> same = em.createQuery("select b from Brand b where lower(b.name) = :n", Brand.class)
                .setParameter("n", bn.toLowerCase(Locale.ROOT)).getResultList();
        if (!same.isEmpty()) {
            Brand b = same.get(0);
            if (!b.isActive())
                throw new BusinessException("Hãng " + b.getName() + " đang bị tắt.");
            return b;
        }
        Brand b = new Brand();
        b.setName(bn);
        b.setActive(true);
        em.persist(b);
        em.flush();
        return b;
    }

    private String uniqueSlug(String name) {
        String base = ascii(name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isEmpty())
            base = "san-pham";
        if (base.length() > 200)
            base = base.substring(0, 200);
        String slug = base;
        for (int i = 2; products.existsBySlug(slug); i++)
            slug = base + "-" + i;
        return slug;
    }

    private String uniqueSku(Product p, Color c, String gpu, int ram, int storage) {
        StringBuilder b = new StringBuilder(upperAlnum(p.getName(), 16));
        if (b.length() == 0)
            b.append("SP");
        if (gpu != null && !upperAlnum(gpu, 8).isEmpty())
            b.append('-').append(upperAlnum(gpu, 8));
        b.append('-').append(ram).append('-').append(storage);
        String col = upperAlnum(c.getName(), 3);
        b.append('-').append(col.isEmpty() ? "CLR" : col);
        String base = b.toString();
        String sku = base;
        for (int i = 2; variants.existsBySku(sku); i++)
            sku = base + "-" + i;
        return sku;
    }

    private static String variantName(Product p, Color c, String cpu, String gpu, int ram, int storage) {
        String n = p.getName() + " " + cpu + (gpu != null ? " " + gpu : "") + " " + ram + "GB/" + storage + "GB "
                + c.getName();
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    private static String describe(Product p, Color c, String cpu, String gpu, int ram, int storage) {
        return "\"" + variantName(p, c, cpu, gpu, ram, storage) + "\"";
    }

    private static String gpuKey(String gpu) {
        return gpu == null ? "" : gpu.trim().toLowerCase(Locale.ROOT);
    }

    private static String ascii(String s) {
        return Normalizer.normalize(s.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static String upperAlnum(String s, int max) {
        String x = ascii(s).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return x.length() > max ? x.substring(0, max) : x;
    }

    /** Tạo mới sản phẩm/biến thể cần PRODUCT_MANAGE (phiếu nhập chỉ dùng hàng có sẵn thì không cần). */
    private static void requireManage() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean ok = a != null && a.getAuthorities().stream().anyMatch(x -> "PRODUCT_MANAGE".equals(x.getAuthority()));
        if (!ok)
            throw new BusinessException("Bạn không có quyền tạo sản phẩm hoặc cấu hình mới (cần quyền PRODUCT_MANAGE).");
    }

    private AppUser user(String username) {
        List<AppUser> found = em.createQuery(
                "select u from AppUser u where u.username = :n and u.deletedAt is null", AppUser.class)
                .setParameter("n", username).getResultList();
        if (found.isEmpty())
            throw new BusinessException("Không xác định được người thao tác.");
        return found.get(0);
    }
}
