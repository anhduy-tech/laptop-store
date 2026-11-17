package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.sales.domain.Campaign;
import com.lapstore.sales.domain.CampaignScope;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherCustomer;
import com.lapstore.sales.dto.VoucherSuggestion;
import com.lapstore.sales.pricing.PricingEngine;
import com.lapstore.sales.pricing.PricingEngine.CampaignRule;
import com.lapstore.sales.pricing.PricingEngine.Scope;
import com.lapstore.sales.pricing.PricingEngine.VoucherEval;
import com.lapstore.sales.pricing.PricingEngine.VoucherRule;
import com.lapstore.sales.repo.CampaignRepository;
import com.lapstore.sales.repo.CampaignScopeRepository;
import com.lapstore.sales.repo.VoucherCustomerRepository;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.sales.repo.VoucherRepository;

/**
 * Nạp dữ liệu giảm giá cho {@link PricingEngine}, kiểm tra điều kiện của voucher (trạng thái, thời hạn, hạn mức, khách) và
 * gợi ý voucher. Không ghi gì: việc giữ/nhả/chốt lượt voucher nằm ở CounterTx. Dùng chung cho bán quầy (sau này cho đơn online).
 */
@Service
public class PricingService {
    private static final List<String> COUNTED = List.of("RESERVED", "USED");

    private final CampaignRepository campaigns;
    private final CampaignScopeRepository scopes;
    private final VoucherRepository vouchers;
    private final VoucherRedemptionRepository redemptions;
    private final VoucherCustomerRepository personal;

    PricingService(CampaignRepository campaigns, CampaignScopeRepository scopes, VoucherRepository vouchers,
                   VoucherRedemptionRepository redemptions, VoucherCustomerRepository personal) {
        this.campaigns = campaigns;
        this.scopes = scopes;
        this.vouchers = vouchers;
        this.redemptions = redemptions;
        this.personal = personal;
    }

    /** Các campaign đang chạy (ACTIVE, trong thời hạn) kèm phạm vi. */
    @Transactional(readOnly = true)
    public List<CampaignRule> runningCampaigns(Instant now) {
        List<Campaign> list = campaigns.findRunning(now);
        if (list.isEmpty()) return List.of();
        Map<Long, List<Scope>> byOwner = scopes
                .findByOwnerTypeAndOwnerIdIn("CAMPAIGN", list.stream().map(Campaign::getId).toList()).stream()
                .collect(Collectors.groupingBy(CampaignScope::getOwnerId,
                        Collectors.mapping(s -> new Scope(s.getScopeType(), s.getTargetId(), s.isExclude()), Collectors.toList())));
        List<CampaignRule> out = new ArrayList<>();
        for (Campaign c : list) {
            out.add(new CampaignRule(c.getId(), c.getCode(), c.getName(), c.getDiscountType(), c.getDiscountValue().longValue(),
                    c.getMaxDiscount() == null ? null : c.getMaxDiscount().longValue(),
                    c.getMinOrderValue() == null ? 0 : c.getMinOrderValue().longValue(),
                    c.getPriority() == null ? 0 : c.getPriority(), c.isStackable(), byOwner.getOrDefault(c.getId(), List.of())));
        }
        return out;
    }

    public static VoucherRule rule(Voucher v) {
        return new VoucherRule(v.getId(), v.getCode(), v.getName(), v.getDiscountType(), v.getDiscountValue().longValue(),
                v.getMaxDiscount() == null ? null : v.getMaxDiscount().longValue(),
                v.getMinOrder() == null ? 0 : v.getMinOrder().longValue(), v.isStackableWithCampaign());
    }

    /**
     * Lý do voucher KHÔNG dùng được cho khách/đơn này, hoặc null nếu dùng được.
     *
     * @param heldByThisOrder true nếu đơn đang xét đã giữ một lượt của voucher (để không tự chặn chính mình)
     * @param orderId         đơn đang xét, loại khỏi phép đếm lượt của khách
     */
    @Transactional(readOnly = true)
    public String blocker(Voucher v, Customer customer, String phone, long orderId, boolean heldByThisOrder, Instant now) {
        if (!"ACTIVE".equals(v.getStatus())) return "Voucher đã ngừng áp dụng";
        if (v.getStartAt().isAfter(now)) return "Voucher chưa đến thời gian áp dụng";
        if (!v.getEndAt().isAfter(now)) return "Voucher đã hết hạn";
        if (v.getTotalLimit() != null) {
            int taken = v.getUsedCount() + v.getReservedCount() - (heldByThisOrder ? 1 : 0);
            if (taken >= v.getTotalLimit()) return "Voucher đã hết lượt";
        }
        if ("PERSONAL".equals(v.getAudience())) {
            if (customer == null) return "Voucher dành riêng cho khách — hãy chọn khách hàng";
            boolean owned = personal.findByVoucher_IdAndCustomer_Id(v.getId(), customer.getId()).stream()
                    .anyMatch(vc -> "ACTIVE".equals(vc.getStatus()) && (vc.getExpiresAt() == null || vc.getExpiresAt().isAfter(now)));
            if (!owned) return "Voucher không dành cho khách này";
        }
        if (phone != null && !"-".equals(phone) && v.getPerCustomerLimit() != null) {
            long used = redemptions.countByVoucher_IdAndCustomerPhoneAndStatusInAndOrder_IdNot(v.getId(), phone, COUNTED, orderId);
            if (used >= v.getPerCustomerLimit()) return "Khách này đã dùng hết lượt của voucher";
        }
        return null;
    }

    /**
     * Gợi ý voucher cho đơn: dùng được xếp trước (giảm nhiều nhất lên đầu, đánh dấu best), rồi tới các voucher còn thiếu điều
     * kiện kèm lý do (vd cần mua thêm bao nhiêu). Voucher hết lượt/hết hạn/không thuộc khách bị ẩn.
     */
    @Transactional(readOnly = true)
    public List<VoucherSuggestion> suggest(long base, long campaignDiscount, Customer customer, String phone, long orderId,
                                           Long appliedVoucherId, Instant now) {
        record Row(VoucherSuggestion s, VoucherEval e) {}
        List<Row> rows = new ArrayList<>();
        for (Voucher v : vouchers.findOpen(now)) {
            boolean applied = v.getId().equals(appliedVoucherId);
            if (blocker(v, customer, phone, orderId, applied, now) != null) continue;
            VoucherRule r = rule(v);
            VoucherEval e = PricingEngine.evalVoucher(r, base, campaignDiscount);
            rows.add(new Row(new VoucherSuggestion(v.getCode(), v.getName(), PricingEngine.describe(r), e.discount(),
                    e.eligible(), e.reason(), false, applied), e));
        }
        long bestAmount = rows.stream().filter(x -> x.e().eligible()).mapToLong(x -> x.e().discount()).max().orElse(0);
        List<VoucherSuggestion> out = new ArrayList<>();
        rows.stream().filter(x -> x.e().eligible())
                .sorted(Comparator.comparingLong((Row x) -> x.e().discount()).reversed())
                .forEach(x -> out.add(mark(x.s(), x.s().discount() == bestAmount)));
        rows.stream().filter(x -> !x.e().eligible()).map(Row::s).forEach(out::add);
        return out;
    }

    private static VoucherSuggestion mark(VoucherSuggestion s, boolean best) {
        return new VoucherSuggestion(s.code(), s.name(), s.description(), s.discount(), s.eligible(), s.reason(), best, s.applied());
    }

    public static long toLong(BigDecimal v) {
        return v == null ? 0 : v.longValue();
    }
}
