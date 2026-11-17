package com.lapstore.sales.pricing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tính giảm giá thuần (không đụng DB, dễ test): campaign trước, voucher sau.
 * <ul>
 *   <li>PERCENT: % trên từng dòng. AMOUNT: số tiền cố định cho cả đơn, chia theo tỉ lệ giá trị các dòng thuộc phạm vi.
 *       FIXED_PRICE: đơn giá về đúng giá trị đặt.</li>
 *   <li>{@code max}: trần giảm tối đa của riêng campaign đó (cắt theo tỉ lệ các dòng).</li>
 *   <li>Mỗi dòng: chỉ một campaign KHÔNG cộng dồn (cái giảm nhiều nhất, hòa thì ưu tiên priority cao), cộng với mọi
 *       campaign cộng dồn được. Tổng giảm của dòng không vượt giá trị dòng.</li>
 *   <li>Voucher tính trên phần còn lại sau campaign; chỉ dùng chung với campaign khi {@code stackable}.</li>
 * </ul>
 * Số tiền là đồng (long), làm tròn xuống.
 */
public final class PricingEngine {
    private PricingEngine() {}

    public record Scope(String type, Long targetId, boolean exclude) {}

    /** key: định danh dòng do bên gọi chọn (vd id hold). Các id null được coi là 0. */
    public record Line(long key, long variantId, long productId, long brandId, long categoryId, long unitPrice, int qty) {
        public long total() { return unitPrice * qty; }
    }

    public record CampaignRule(long id, String code, String name, String type, long value, Long max, long minOrder,
                               int priority, boolean stackable, List<Scope> scopes) {}

    public record VoucherRule(long id, String code, String name, String type, long value, Long max, long minOrder,
                              boolean stackable) {}

    /** Một khoản giảm của campaign trên một dòng. */
    public record Applied(long campaignId, String name, String rule, long lineKey, long amount) {}

    public record Result(long subtotal, long campaignDiscount, Map<Long, Long> lineDiscount, List<Applied> applied) {}

    public record VoucherEval(boolean eligible, long discount, String reason) {}

    public static Result price(List<Line> lines, List<CampaignRule> campaigns) {
        long subtotal = 0;
        for (Line l : lines) subtotal += l.total();

        // 1) Mỗi campaign: số giảm theo từng dòng (đã áp trần).
        Map<Long, Map<Long, Long>> perCampaign = new LinkedHashMap<>();
        Map<Long, CampaignRule> byId = new LinkedHashMap<>();
        for (CampaignRule c : campaigns) {
            if (subtotal < c.minOrder()) continue;
            List<Line> eligible = new ArrayList<>();
            for (Line l : lines) if (matches(c.scopes(), l)) eligible.add(l);
            if (eligible.isEmpty()) continue;
            Map<Long, Long> raw = rawDiscount(c, eligible);
            long sum = 0;
            for (long v : raw.values()) sum += v;
            if (sum <= 0) continue;
            if (c.max() != null && c.max() > 0 && sum > c.max()) {
                Map<Long, Long> scaled = new LinkedHashMap<>();
                long given = 0;
                for (var e : raw.entrySet()) {
                    long part = e.getValue() * c.max() / sum;
                    scaled.put(e.getKey(), part);
                    given += part;
                }
                // phần lẻ do làm tròn xuống: dồn vào dòng đầu để tổng đúng bằng trần
                Long first = scaled.keySet().iterator().next();
                scaled.put(first, scaled.get(first) + (c.max() - given));
                raw = scaled;
            }
            raw.values().removeIf(v -> v <= 0);
            if (raw.isEmpty()) continue;
            perCampaign.put(c.id(), raw);
            byId.put(c.id(), c);
        }

        // 2) Mỗi dòng: một campaign không cộng dồn tốt nhất + mọi campaign cộng dồn.
        Map<Long, Long> lineDiscount = new LinkedHashMap<>();
        List<Applied> applied = new ArrayList<>();
        long campaignTotal = 0;
        for (Line l : lines) {
            CampaignRule best = null;
            long bestAmt = 0;
            List<CampaignRule> chosen = new ArrayList<>();
            for (var e : perCampaign.entrySet()) {
                Long amt = e.getValue().get(l.key());
                if (amt == null) continue;
                CampaignRule c = byId.get(e.getKey());
                if (c.stackable()) { chosen.add(c); continue; }
                if (best == null || amt > bestAmt || (amt == bestAmt && c.priority() > best.priority())) {
                    best = c;
                    bestAmt = amt;
                }
            }
            if (best != null) chosen.add(0, best);
            long remaining = l.total();
            long lineSum = 0;
            for (CampaignRule c : chosen) {
                long amt = Math.min(perCampaign.get(c.id()).get(l.key()), remaining);
                if (amt <= 0) continue;
                remaining -= amt;
                lineSum += amt;
                applied.add(new Applied(c.id(), c.name(), describe(c), l.key(), amt));
            }
            if (lineSum > 0) {
                lineDiscount.put(l.key(), lineSum);
                campaignTotal += lineSum;
            }
        }
        return new Result(subtotal, campaignTotal, lineDiscount, applied);
    }

    /**
     * @param base tiền hàng sau campaign; @param campaignDiscount tổng giảm của campaign đang áp
     */
    public static VoucherEval evalVoucher(VoucherRule v, long base, long campaignDiscount) {
        if (campaignDiscount > 0 && !v.stackable())
            return new VoucherEval(false, 0, "Không dùng chung với khuyến mãi đang áp dụng");
        if (base < v.minOrder())
            return new VoucherEval(false, 0, "Cần mua thêm " + money(v.minOrder() - base) + " để dùng");
        long d;
        if ("PERCENT".equals(v.type())) {
            d = base * v.value() / 100;
            if (v.max() != null && v.max() > 0) d = Math.min(d, v.max());
        } else {
            d = v.value();
        }
        d = Math.max(0, Math.min(d, base));
        if (d == 0) return new VoucherEval(false, 0, "Không có khoản giảm nào");
        return new VoucherEval(true, d, null);
    }

    public static String describe(CampaignRule c) {
        return switch (c.type()) {
            case "PERCENT" -> "Giảm " + c.value() + "%" + (c.max() != null ? ", tối đa " + money(c.max()) : "");
            case "FIXED_PRICE" -> "Đồng giá " + money(c.value());
            default -> "Giảm " + money(c.value());
        };
    }

    public static String describe(VoucherRule v) {
        String s = "PERCENT".equals(v.type()) ? "Giảm " + v.value() + "%" : "Giảm " + money(v.value());
        if ("PERCENT".equals(v.type()) && v.max() != null) s += ", tối đa " + money(v.max());
        if (v.minOrder() > 0) s += ", đơn từ " + money(v.minOrder());
        return s;
    }

    public static String money(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append('.');
            b.append(s.charAt(i));
        }
        return (v < 0 ? "-" : "") + b + "đ";
    }

    // ------------------------------------------------------------------ nội bộ

    private static Map<Long, Long> rawDiscount(CampaignRule c, List<Line> eligible) {
        Map<Long, Long> raw = new LinkedHashMap<>();
        switch (c.type()) {
            case "PERCENT" -> {
                for (Line l : eligible) raw.put(l.key(), l.total() * c.value() / 100);
            }
            case "FIXED_PRICE" -> {
                for (Line l : eligible) raw.put(l.key(), Math.max(0, l.unitPrice() - c.value()) * l.qty());
            }
            default -> { // AMOUNT: chia theo tỉ lệ
                long total = 0;
                for (Line l : eligible) total += l.total();
                long given = 0;
                for (int i = 0; i < eligible.size(); i++) {
                    Line l = eligible.get(i);
                    long part = (i == eligible.size() - 1) ? c.value() - given : c.value() * l.total() / total;
                    part = Math.max(0, Math.min(part, l.total()));
                    given += part;
                    raw.put(l.key(), part);
                }
            }
        }
        return raw;
    }

    static boolean matches(List<Scope> scopes, Line l) {
        if (scopes == null || scopes.isEmpty()) return true;
        boolean anyInclude = false;
        boolean includeHit = false;
        for (Scope s : scopes) {
            boolean hit = hit(s, l);
            if (s.exclude()) {
                if (hit) return false;
            } else {
                anyInclude = true;
                if (hit) includeHit = true;
            }
        }
        return !anyInclude || includeHit;
    }

    private static boolean hit(Scope s, Line l) {
        long t = s.targetId() == null ? 0 : s.targetId();
        return switch (s.type()) {
            case "ALL" -> true;
            case "BRAND" -> l.brandId() == t;
            case "CATEGORY" -> l.categoryId() == t;
            case "PRODUCT" -> l.productId() == t;
            case "VARIANT" -> l.variantId() == t;
            default -> false; // GROUP: chưa hỗ trợ
        };
    }

    /** Sắp xếp gợi ý voucher: giảm nhiều nhất trước. */
    public static final Comparator<VoucherEval> BY_DISCOUNT_DESC = Comparator.comparingLong(VoucherEval::discount).reversed();
}
