package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.dto.IdName;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.sales.domain.Campaign;
import com.lapstore.sales.domain.CampaignScope;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.dto.CampaignRequest;
import com.lapstore.sales.dto.CampaignRow;
import com.lapstore.sales.dto.ScopeOptions;
import com.lapstore.sales.dto.VoucherRequest;
import com.lapstore.sales.dto.VoucherRow;
import com.lapstore.sales.pricing.PricingEngine;
import com.lapstore.sales.repo.CampaignRepository;
import com.lapstore.sales.repo.CampaignScopeRepository;
import com.lapstore.sales.repo.VoucherRepository;

import jakarta.persistence.EntityManager;

/**
 * Campaign: ACTIVE nếu đã tới ngày bắt đầu, SCHEDULED nếu hẹn ngày tới; tạm dừng = CANCELLED. Chưa có bước duyệt riêng
 * (cột approved_by để trống). Hiển thị trạng thái theo thời gian thực: ACTIVE nhưng quá hạn hiện "Đã kết thúc".
 * Phạm vi hỗ trợ: ALL, BRAND, CATEGORY. Voucher: audience PUBLIC; tạm dừng = CLOSED.
 */
@Service
class CampaignServiceImpl implements CampaignService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> TYPES = Set.of("PERCENT", "AMOUNT", "FIXED_PRICE");

    private final CampaignRepository campaigns;
    private final CampaignScopeRepository scopes;
    private final VoucherRepository vouchers;
    private final EntityManager em;

    CampaignServiceImpl(CampaignRepository campaigns, CampaignScopeRepository scopes, VoucherRepository vouchers, EntityManager em) {
        this.campaigns = campaigns;
        this.scopes = scopes;
        this.vouchers = vouchers;
        this.em = em;
    }

    @Override
    @PreAuthorize("hasAuthority('CAMPAIGN_MANAGE')")
    @Transactional(readOnly = true)
    public List<CampaignRow> campaigns() {
        Instant now = Instant.now();
        List<Campaign> list = campaigns.findAllByOrderByIdDesc();
        Map<Long, String> scopeText = scopeTexts(list);
        return list.stream().map(c -> new CampaignRow(c.getId(), c.getCode(), c.getName(), typeLabel(c.getDiscountType()),
                valueText(c), scopeText.getOrDefault(c.getId(), "Tất cả sản phẩm"), period(c.getStartAt(), c.getEndAt()),
                campaignStatus(c, now), c.getPriority() == null ? 0 : c.getPriority(), c.isStackable(),
                !"ENDED".equals(campaignStatus(c, now)) && !c.getEndAt().isBefore(now))).toList();
    }

    @Override
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    @Transactional(readOnly = true)
    public List<VoucherRow> vouchers() {
        Instant now = Instant.now();
        return vouchers.findAllByOrderByIdDesc().stream().map(v -> new VoucherRow(v.getId(), v.getCode(), v.getName(),
                voucherValue(v), period(v.getStartAt(), v.getEndAt()), v.getUsedCount(), v.getReservedCount(),
                v.getTotalLimit() == null ? "Không giới hạn" : String.valueOf(v.getTotalLimit()), voucherStatus(v, now),
                v.isStackableWithCampaign(), v.getEndAt().isAfter(now))).toList();
    }

    @Override
    @PreAuthorize("hasAuthority('CAMPAIGN_MANAGE')")
    @Transactional(readOnly = true)
    public ScopeOptions scopeOptions() {
        List<IdName> brands = em.createQuery("select b.id, b.name from Brand b where b.active = true order by b.name", Object[].class)
                .getResultList().stream().map(r -> new IdName((Long) r[0], (String) r[1])).toList();
        List<IdName> cats = em.createQuery("select c.id, c.name from Category c where c.active = true order by c.name", Object[].class)
                .getResultList().stream().map(r -> new IdName((Long) r[0], (String) r[1])).toList();
        return new ScopeOptions(brands, cats);
    }

    @Override
    @PreAuthorize("hasAuthority('CAMPAIGN_MANAGE')")
    @Transactional
    public void createCampaign(CampaignRequest r, String username) {
        if (r.name() == null || r.name().isBlank()) throw new BusinessException("Nhập tên campaign.");
        if (!TYPES.contains(r.type())) throw new BusinessException("Loại giảm giá không hợp lệ.");
        if (r.value() == null || r.value() <= 0) throw new BusinessException("Giá trị giảm phải lớn hơn 0.");
        if ("PERCENT".equals(r.type()) && r.value() > 100) throw new BusinessException("Giảm theo % không được quá 100.");
        if (r.startAt() == null || r.endAt() == null || r.endAt().isBefore(r.startAt()))
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        String scopeType = r.scopeType() == null ? "ALL" : r.scopeType();
        if (!Set.of("ALL", "BRAND", "CATEGORY").contains(scopeType)) throw new BusinessException("Phạm vi không hợp lệ.");
        if (!"ALL".equals(scopeType) && r.scopeTargetId() == null) throw new BusinessException("Chọn hãng/danh mục áp dụng.");

        Instant start = r.startAt().atStartOfDay(ZONE).toInstant();
        Instant end = r.endAt().plusDays(1).atStartOfDay(ZONE).toInstant(); // hết ngày kết thúc
        Instant now = Instant.now();

        Campaign c = new Campaign();
        c.setCode(newCode("CP", now));
        c.setName(r.name().trim());
        c.setDiscountType(r.type());
        c.setDiscountValue(BigDecimal.valueOf(r.value()));
        c.setMaxDiscount(r.maxDiscount() == null || r.maxDiscount() <= 0 ? null : BigDecimal.valueOf(r.maxDiscount()));
        c.setMinOrderValue(BigDecimal.valueOf(r.minOrder() == null ? 0 : r.minOrder()));
        c.setPriority(r.priority() == null ? 0 : r.priority());
        c.setStackable(r.stackable());
        c.setStatus(start.isAfter(now) ? "SCHEDULED" : "ACTIVE");
        c.setStartAt(start);
        c.setEndAt(end);
        campaigns.save(c);

        CampaignScope s = new CampaignScope();
        s.setOwnerType("CAMPAIGN");
        s.setOwnerId(c.getId());
        s.setScopeType(scopeType);
        s.setTargetId("ALL".equals(scopeType) ? null : r.scopeTargetId());
        s.setExclude(false);
        scopes.save(s);
    }

    @Override
    @PreAuthorize("hasAuthority('CAMPAIGN_MANAGE')")
    @Transactional
    public void setCampaignActive(long id, boolean active, String username) {
        Campaign c = campaigns.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy campaign."));
        Instant now = Instant.now();
        if (!c.getEndAt().isAfter(now)) throw new BusinessException("Campaign đã kết thúc, không thể đổi trạng thái.");
        c.setStatus(active ? (c.getStartAt().isAfter(now) ? "SCHEDULED" : "ACTIVE") : "CANCELLED");
    }

    @Override
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    @Transactional
    public void createVoucher(VoucherRequest r, String username) {
        String code = r.code() == null ? "" : r.code().trim().toUpperCase();
        if (!code.matches("[A-Z0-9_-]{3,30}")) throw new BusinessException("Mã voucher gồm 3–30 ký tự chữ, số, gạch.");
        if (vouchers.findByCodeIgnoreCase(code).isPresent()) throw new BusinessException("Mã voucher " + code + " đã tồn tại.");
        if (r.name() == null || r.name().isBlank()) throw new BusinessException("Nhập tên voucher.");
        if (!Set.of("PERCENT", "AMOUNT").contains(r.type())) throw new BusinessException("Loại giảm giá không hợp lệ.");
        if (r.value() == null || r.value() <= 0) throw new BusinessException("Giá trị giảm phải lớn hơn 0.");
        if ("PERCENT".equals(r.type()) && r.value() > 100) throw new BusinessException("Giảm theo % không được quá 100.");
        if (r.startAt() == null || r.endAt() == null || r.endAt().isBefore(r.startAt()))
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        if (r.totalLimit() != null && r.totalLimit() <= 0) throw new BusinessException("Giới hạn lượt phải lớn hơn 0.");

        Voucher v = new Voucher();
        v.setCode(code);
        v.setName(r.name().trim());
        v.setAudience("PUBLIC");
        v.setDiscountType(r.type());
        v.setDiscountValue(BigDecimal.valueOf(r.value()));
        v.setMaxDiscount(r.maxDiscount() == null || r.maxDiscount() <= 0 ? null : BigDecimal.valueOf(r.maxDiscount()));
        v.setMinOrder(BigDecimal.valueOf(r.minOrder() == null ? 0 : r.minOrder()));
        v.setTotalLimit(r.totalLimit());
        v.setPerCustomerLimit(r.perCustomerLimit() == null || r.perCustomerLimit() <= 0 ? 1 : r.perCustomerLimit());
        v.setUsedCount(0);
        v.setReservedCount(0);
        v.setStackableWithCampaign(r.stackableWithCampaign());
        v.setStartAt(r.startAt().atStartOfDay(ZONE).toInstant());
        v.setEndAt(r.endAt().plusDays(1).atStartOfDay(ZONE).toInstant());
        v.setStatus("ACTIVE");
        vouchers.save(v);
    }

    @Override
    @PreAuthorize("hasAuthority('VOUCHER_MANAGE')")
    @Transactional
    public void setVoucherActive(long id, boolean active, String username) {
        Voucher v = vouchers.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy voucher."));
        if (!v.getEndAt().isAfter(Instant.now())) throw new BusinessException("Voucher đã hết hạn, không thể đổi trạng thái.");
        v.setStatus(active ? "ACTIVE" : "CLOSED");
    }

    // ------------------------------------------------------------------ hiển thị

    private Map<Long, String> scopeTexts(List<Campaign> list) {
        if (list.isEmpty()) return Map.of();
        Map<Long, String> out = new HashMap<>();
        for (CampaignScope s : scopes.findByOwnerTypeAndOwnerIdIn("CAMPAIGN", list.stream().map(Campaign::getId).toList())) {
            String t = switch (s.getScopeType()) {
                case "BRAND" -> "Hãng " + nameOf("Brand", s.getTargetId());
                case "CATEGORY" -> "Danh mục " + nameOf("Category", s.getTargetId());
                case "PRODUCT" -> "Sản phẩm #" + s.getTargetId();
                case "VARIANT" -> "Cấu hình #" + s.getTargetId();
                default -> "Tất cả sản phẩm";
            };
            out.merge(s.getOwnerId(), (s.isExclude() ? "Trừ " : "") + t, (a, b) -> a + "; " + b);
        }
        return out;
    }

    private String nameOf(String entity, Long id) {
        if (id == null) return "?";
        List<String> l = em.createQuery("select e.name from " + entity + " e where e.id = :id", String.class)
                .setParameter("id", id).getResultList();
        return l.isEmpty() ? "#" + id : l.get(0);
    }

    private static String campaignStatus(Campaign c, Instant now) {
        if ("CANCELLED".equals(c.getStatus()) || "DRAFT".equals(c.getStatus())) return c.getStatus();
        if (!c.getEndAt().isAfter(now)) return "ENDED";
        if (c.getStartAt().isAfter(now)) return "SCHEDULED";
        return "ACTIVE";
    }

    private static String voucherStatus(Voucher v, Instant now) {
        if (!"ACTIVE".equals(v.getStatus())) return v.getStatus();
        if (!v.getEndAt().isAfter(now)) return "ENDED";
        if (v.getStartAt().isAfter(now)) return "SCHEDULED";
        if (v.getTotalLimit() != null && v.getUsedCount() + v.getReservedCount() >= v.getTotalLimit()) return "CLOSED";
        return "ACTIVE";
    }

    private static String typeLabel(String t) {
        return switch (t) {
            case "PERCENT" -> "Giảm %";
            case "AMOUNT" -> "Giảm tiền";
            default -> "Đồng giá";
        };
    }

    private static String valueText(Campaign c) {
        long v = PricingService.toLong(c.getDiscountValue());
        String s = switch (c.getDiscountType()) {
            case "PERCENT" -> v + "%";
            case "FIXED_PRICE" -> PricingEngine.money(v);
            default -> PricingEngine.money(v);
        };
        if (c.getMaxDiscount() != null) s += " (tối đa " + PricingEngine.money(c.getMaxDiscount().longValue()) + ")";
        long min = PricingService.toLong(c.getMinOrderValue());
        if (min > 0) s += ", đơn từ " + PricingEngine.money(min);
        return s;
    }

    private static String voucherValue(Voucher v) {
        long val = PricingService.toLong(v.getDiscountValue());
        String s = "PERCENT".equals(v.getDiscountType()) ? val + "%" : PricingEngine.money(val);
        if (v.getMaxDiscount() != null) s += " (tối đa " + PricingEngine.money(v.getMaxDiscount().longValue()) + ")";
        long min = PricingService.toLong(v.getMinOrder());
        if (min > 0) s += ", đơn từ " + PricingEngine.money(min);
        return s;
    }

    private static String period(Instant from, Instant to) {
        LocalDate a = from.atZone(ZONE).toLocalDate();
        LocalDate b = to.minusSeconds(1).atZone(ZONE).toLocalDate();
        return a.format(DAY) + " – " + b.format(DAY);
    }

    private static String newCode(String prefix, Instant now) {
        return prefix + now.atZone(ZONE).format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + java.util.UUID.randomUUID().toString().substring(0, 3).toUpperCase();
    }
}
