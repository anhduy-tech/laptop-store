package com.lapstore.sales.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lapstore.sales.pricing.PricingEngine.CampaignRule;
import com.lapstore.sales.pricing.PricingEngine.Line;
import com.lapstore.sales.pricing.PricingEngine.Result;
import com.lapstore.sales.pricing.PricingEngine.Scope;
import com.lapstore.sales.pricing.PricingEngine.VoucherEval;
import com.lapstore.sales.pricing.PricingEngine.VoucherRule;

/** Test thuần (không cần Spring/DB) cho tính giảm giá của bán quầy. */
class PricingEngineTest {
    private static final List<Scope> ALL = List.of(new Scope("ALL", null, false));
    private static final Line ASUS = new Line(1, 10, 100, 5, 7, 20_000_000, 1);
    private static final Line ACER = new Line(2, 11, 101, 6, 7, 10_000_000, 1);

    private static CampaignRule percent(long id, int pct, Long max, long min, int prio, boolean stack, List<Scope> sc) {
        return new CampaignRule(id, "C" + id, "C" + id, "PERCENT", pct, max, min, prio, stack, sc);
    }

    @Test
    void percentCappedByMaxScalesLinesAndKeepsTotalExact() {
        Result r = PricingEngine.price(List.of(ASUS, ACER), List.of(percent(1, 8, 2_000_000L, 10_000_000, 10, false, ALL)));
        assertThat(r.subtotal()).isEqualTo(30_000_000L);
        assertThat(r.campaignDiscount()).isEqualTo(2_000_000L);
        assertThat(r.lineDiscount().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(2_000_000L);
    }

    @Test
    void minOrderBlocksCampaign() {
        Result r = PricingEngine.price(List.of(new Line(3, 1, 1, 1, 1, 5_000_000, 1)),
                List.of(percent(1, 8, null, 10_000_000, 10, false, ALL)));
        assertThat(r.campaignDiscount()).isZero();
    }

    @Test
    void stackableAddsOnTopOfBestNonStackable() {
        CampaignRule bts = percent(1, 8, null, 0, 10, false, ALL);
        CampaignRule bf = new CampaignRule(2, "BF", "BF", "AMOUNT", 1_000_000, null, 0, 20, true,
                List.of(new Scope("BRAND", 5L, false)));
        Result r = PricingEngine.price(List.of(ASUS, ACER), List.of(bts, bf));
        assertThat(r.lineDiscount().get(1L)).isEqualTo(1_600_000L + 1_000_000L); // ASUS: 8% + 1tr
        assertThat(r.lineDiscount().get(2L)).isEqualTo(800_000L);               // Acer: chỉ 8%
    }

    @Test
    void onlyBestNonStackablePerLine() {
        Result r = PricingEngine.price(List.of(ASUS),
                List.of(percent(1, 8, null, 0, 10, false, ALL), percent(2, 20, null, 0, 1, false, ALL)));
        assertThat(r.campaignDiscount()).isEqualTo(4_000_000L);
        assertThat(r.applied()).hasSize(1);
    }

    @Test
    void fixedPriceAndExcludeScope() {
        CampaignRule fp = new CampaignRule(4, "FP", "FP", "FIXED_PRICE", 18_000_000, null, 0, 1, false, ALL);
        assertThat(PricingEngine.price(List.of(ASUS), List.of(fp)).campaignDiscount()).isEqualTo(2_000_000L);

        CampaignRule ex = percent(5, 10, null, 0, 1, false, List.of(new Scope("ALL", null, false), new Scope("BRAND", 5L, true)));
        Result r = PricingEngine.price(List.of(ASUS, ACER), List.of(ex));
        assertThat(r.lineDiscount()).doesNotContainKey(1L);
        assertThat(r.lineDiscount().get(2L)).isEqualTo(1_000_000L);
    }

    @Test
    void voucherRules() {
        VoucherRule lap10 = new VoucherRule(1, "LAP10", "", "PERCENT", 10, 3_000_000L, 0, false);
        assertThat(PricingEngine.evalVoucher(lap10, 30_000_000, 0).discount()).isEqualTo(3_000_000L);
        assertThat(PricingEngine.evalVoucher(lap10, 30_000_000, 100).eligible()).isFalse(); // không dùng chung KM

        VoucherRule nu = new VoucherRule(2, "NEW", "", "AMOUNT", 500_000, null, 10_000_000, true);
        VoucherEval ev = PricingEngine.evalVoucher(nu, 8_000_000, 0);
        assertThat(ev.eligible()).isFalse();
        assertThat(ev.reason()).isEqualTo("Cần mua thêm 2.000.000đ để dùng");
        assertThat(PricingEngine.evalVoucher(nu, 10_000_000, 0).discount()).isEqualTo(500_000L);
    }
}
