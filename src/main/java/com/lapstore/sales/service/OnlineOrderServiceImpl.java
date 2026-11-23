package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.sales.domain.Cart;
import com.lapstore.sales.domain.CartItem;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderDiscountLine;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.OrderStatusHistory;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.dto.CheckoutRequest;
import com.lapstore.sales.pricing.PricingEngine;
import com.lapstore.sales.pricing.PricingEngine.Applied;
import com.lapstore.sales.pricing.PricingEngine.Line;
import com.lapstore.sales.pricing.PricingEngine.Result;
import com.lapstore.sales.pricing.PricingEngine.VoucherEval;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.VoucherRepository;

import jakarta.persistence.EntityManager;

/**
 * Chốt đơn online (khách không cần đăng nhập). MỘT transaction: chọn chi nhánh còn đủ hàng cho cả giỏ, tính giá + campaign
 * (snapshot, BR-05/06), giữ lượt voucher (RESERVED), tạo đơn NEW + order_item + hold QUANTITY.
 *
 * Giữ số lượng bằng {@code addHeld} (một lệnh UPDATE có điều kiện {@code held_qty <= on_hand}) nên hai khách tranh những máy
 * cuối thì chỉ một bên thành công mà không cần KeyedLock; bên thua nhận "hết hàng" và cả transaction rollback (voucher cũng trả).
 * Hold QUANTITY hết hạn sẽ được HoldExpiryJob nhả; nhân viên xác nhận đơn thì giữ lại (OrderService.confirm).
 */
@Service
class OnlineOrderServiceImpl implements OnlineOrderService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final EntityManager em;
    private final InventoryBalanceRepository balances;
    private final PriceHistoryRepository prices;
    private final PricingService pricing;
    private final VoucherRepository vouchers;
    private final CustomerRepository customers;
    private final ApplicationEventPublisher events;
    private final Duration holdTime;

    OnlineOrderServiceImpl(EntityManager em, InventoryBalanceRepository balances, PriceHistoryRepository prices,
            PricingService pricing, VoucherRepository vouchers, CustomerRepository customers,
            ApplicationEventPublisher events, @Value("${app.hold.online.minutes:1440}") long onlineMinutes) {
        this.em = em;
        this.balances = balances;
        this.prices = prices;
        this.pricing = pricing;
        this.vouchers = vouchers;
        this.customers = customers;
        this.events = events;
        this.holdTime = Duration.ofMinutes(onlineMinutes);
    }

    @Override
    @Transactional
    public String placeOrder(String cartToken, CheckoutRequest req) {
        Instant now = Instant.now();

        // 1) Giỏ
        List<Cart> carts = em.createQuery("select c from Cart c where c.cartToken = :t and c.status = 'ACTIVE'", Cart.class)
                .setParameter("t", cartToken == null ? "" : cartToken).setMaxResults(1).getResultList();
        if (carts.isEmpty())
            throw new BusinessException("Giỏ hàng đang trống.");
        Cart cart = carts.get(0);
        List<CartItem> items = em.createQuery("select i from CartItem i join fetch i.variant v join fetch v.product p "
                + "where i.cart.id = :id order by i.id", CartItem.class).setParameter("id", cart.getId()).getResultList();
        if (items.isEmpty())
            throw new BusinessException("Giỏ hàng đang trống.");

        boolean delivery = "DELIVERY".equals(req.receiveMethod());
        if (delivery && (req.address() == null || req.address().isBlank()))
            throw new BusinessException("Vui lòng nhập địa chỉ giao hàng.");

        // 2) Giá (snapshot) và điều kiện bán
        Map<Long, PriceHistory> price = new LinkedHashMap<>();
        List<Line> lines = new ArrayList<>();
        for (CartItem ci : items) {
            ProductVariant v = ci.getVariant();
            if (!"ACTIVE".equals(v.getStatus()))
                throw new BusinessException(v.getProduct().getName() + " · " + v.getVariantName() + " hiện không bán.");
            List<PriceHistory> eff = prices.findEffective(v.getId(), now);
            if (eff.isEmpty())
                throw new BusinessException("Chưa có giá bán cho " + v.getVariantName() + ".");
            price.put(v.getId(), eff.get(0));
            Product p = v.getProduct();
            lines.add(new Line(v.getId(), v.getId(), p.getId(), p.getBrand() == null ? 0 : p.getBrand().getId(),
                    p.getCategory() == null ? 0 : p.getCategory().getId(), eff.get(0).getPrice().longValue(),
                    ci.getQuantity()));
        }
        Result res = PricingEngine.price(lines, pricing.runningCampaigns(now));
        if (req.expectedSubtotal() != null && req.expectedSubtotal() != res.subtotal()) {
            throw new BusinessException("Giá sản phẩm đã thay đổi. Vui lòng kiểm tra giá mới trước khi đặt hàng.");
        }

        // 3) Khách (theo SĐT) và voucher
        String phone = req.phone().trim();
        Customer customer = customers.findByPhone(phone).orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setFullName(req.fullName().trim());
            customer.setEmail(req.email());
            customers.save(customer);
        }
        Voucher voucher = null;
        VoucherEval ev = null;
        if (req.voucherCode() != null && !req.voucherCode().isBlank()) {
            String code = req.voucherCode().trim();
            voucher = vouchers.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy voucher " + code + "."));
            String blocked = pricing.blocker(voucher, customer, phone, 0, false, now);
            if (blocked != null)
                throw new BusinessException(blocked + ".");
            ev = PricingEngine.evalVoucher(PricingService.rule(voucher), res.subtotal() - res.campaignDiscount(),
                    res.campaignDiscount());
            if (!ev.eligible())
                throw new BusinessException("Chưa dùng được voucher " + voucher.getCode() + ": " + ev.reason() + ".");
        }
        long voucherDiscount = ev == null ? 0 : ev.discount();
        long total = res.subtotal() - res.campaignDiscount() - voucherDiscount;
        if (total <= 0)
            throw new BusinessException("Tổng tiền sau giảm giá không hợp lệ.");

        // 4) Chi nhánh đủ hàng cho cả giỏ
        Branch branch = chooseBranch(items);

        // 5) Đơn
        CustomerOrder o = new CustomerOrder();
        o.setOrderCode(newCode());
        o.setChannel("ONLINE");
        o.setStatus("NEW");
        o.setCustomer(customer);
        o.setCustomerName(req.fullName().trim());
        o.setCustomerPhone(phone);
        o.setCustomerEmail(req.email());
        o.setFulfillmentMethod(delivery ? "DELIVERY" : "PICKUP");
        o.setShipAddressLine(delivery ? req.address() : null);
        o.setFulfillBranch(branch);
        o.setSubtotal(BigDecimal.valueOf(res.subtotal()));
        o.setDiscountTotal(BigDecimal.valueOf(res.campaignDiscount() + voucherDiscount));
        o.setShippingFee(BigDecimal.ZERO);
        o.setTotalAmount(BigDecimal.valueOf(total));
        o.setPaymentStatus("UNPAID");
        o.setPlacedAt(now);
        em.persist(o);

        // 6) order_item + hold QUANTITY (giữ số lượng nguyên tử)
        Map<Long, OrderItem> itemOfVariant = new HashMap<>();
        for (CartItem ci : items) {
            ProductVariant v = ci.getVariant();
            PriceHistory ph = price.get(v.getId());
            long lineDiscount = res.lineDiscount().getOrDefault(v.getId(), 0L);
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setVariant(v);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ph.getPrice());
            oi.setPriceHistory(ph);
            oi.setPricedAt(now);
            oi.setLineDiscount(BigDecimal.valueOf(lineDiscount));
            oi.setLineTotal(ph.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .subtract(BigDecimal.valueOf(lineDiscount)));
            em.persist(oi);
            itemOfVariant.put(v.getId(), oi);

            if (balances.addHeld(v.getId(), branch.getId(), ci.getQuantity(), now) != 1)
                throw new BusinessException("Rất tiếc, " + v.getVariantName() + " vừa hết hàng. Vui lòng giảm số lượng.");
            StockHold h = new StockHold();
            h.setOrder(o);
            h.setOrderItem(oi);
            h.setVariant(v);
            h.setBranch(branch);
            h.setHoldType("QUANTITY");
            h.setQuantity(ci.getQuantity());
            h.setStatus("ACTIVE");
            h.setHeldAt(now);
            h.setExpiresAt(now.plus(holdTime));
            em.persist(h);
        }

        // 7) Dòng giảm giá + lượt voucher
        Map<String, long[]> agg = new LinkedHashMap<>();
        Map<String, Applied> sample = new LinkedHashMap<>();
        for (Applied a : res.applied()) {
            String key = a.campaignId() + ":" + a.lineKey();
            agg.computeIfAbsent(key, k -> new long[1])[0] += a.amount();
            sample.putIfAbsent(key, a);
        }
        for (var e : agg.entrySet()) {
            Applied a = sample.get(e.getKey());
            discountLine(o, itemOfVariant.get(a.lineKey()), "CAMPAIGN", a.campaignId(), a.name(), a.rule(),
                    e.getValue()[0]);
        }
        if (voucher != null) {
            if (vouchers.reserve(voucher.getId()) != 1)
                throw new BusinessException("Voucher " + voucher.getCode() + " đã hết lượt.");
            VoucherRedemption r = new VoucherRedemption();
            r.setVoucher(voucher);
            r.setOrder(o);
            r.setCustomer(customer);
            r.setCustomerPhone(phone);
            r.setStatus("RESERVED");
            r.setDiscountAmount(BigDecimal.valueOf(voucherDiscount));
            r.setReservedAt(now);
            em.persist(r);
            discountLine(o, null, "VOUCHER", voucher.getId(), voucher.getCode() + " – " + voucher.getName(),
                    PricingEngine.describe(PricingService.rule(voucher)), voucherDiscount);
        }

        // 8) Lịch sử, đóng giỏ
        OrderStatusHistory hist = new OrderStatusHistory();
        hist.setOrder(o);
        hist.setToStatus("NEW");
        hist.setChangedAt(now);
        em.persist(hist);
        cart.setStatus("ORDERED");
        if (customer.getFirstOrderAt() == null)
            customer.setFirstOrderAt(now);
        customer.setLastOrderAt(now);
        for (CartItem ci : items)
            events.publishEvent(new StockChanged(ci.getVariant().getId(), branch.getId()));
        return o.getOrderCode();
    }

    /** Chi nhánh đang hoạt động đủ hàng cho MỌI dòng (đơn giao từ một nơi); nhiều nơi đủ thì lấy nơi còn nhiều hàng nhất. */
    private Branch chooseBranch(List<CartItem> items) {
        Map<Long, Integer> need = new HashMap<>();
        for (CartItem ci : items)
            need.merge(ci.getVariant().getId(), ci.getQuantity(), Integer::sum);
        List<Object[]> rows = em.createQuery("select b.id.branchId, b.id.variantId, b.onHand - b.heldQty "
                + "from InventoryBalance b where b.id.variantId in :vs and b.branch.active = true", Object[].class)
                .setParameter("vs", need.keySet()).getResultList();
        Map<Long, Map<Long, Integer>> byBranch = new HashMap<>();
        for (Object[] r : rows)
            byBranch.computeIfAbsent((Long) r[0], k -> new HashMap<>()).put((Long) r[1], ((Number) r[2]).intValue());
        Long best = null;
        long bestSpare = -1;
        for (var e : byBranch.entrySet()) {
            boolean ok = true;
            long spare = 0;
            for (var n : need.entrySet()) {
                int avail = e.getValue().getOrDefault(n.getKey(), 0);
                if (avail < n.getValue()) {
                    ok = false;
                    break;
                }
                spare += avail - n.getValue();
            }
            if (ok && spare > bestSpare) {
                best = e.getKey();
                bestSpare = spare;
            }
        }
        if (best == null)
            throw new BusinessException("Rất tiếc, hiện không có chi nhánh nào còn đủ hàng cho toàn bộ giỏ. "
                    + "Hãy giảm số lượng hoặc tách thành nhiều đơn.");
        return em.getReference(Branch.class, best);
    }

    private void discountLine(CustomerOrder o, OrderItem item, String source, Long sourceId, String name, String rule,
            long amount) {
        OrderDiscountLine d = new OrderDiscountLine();
        d.setOrder(o);
        d.setOrderItem(item);
        d.setSourceType(source);
        d.setSourceId(sourceId);
        d.setNameSnapshot(name.length() > 150 ? name.substring(0, 150) : name);
        d.setRuleSnapshot("{\"rule\":\"" + rule.replace("\\", "/").replace("\"", "'") + "\"}");
        d.setAmount(BigDecimal.valueOf(amount));
        em.persist(d);
    }

    private static String newCode() {
        return "LS" + LocalDate.now(ZONE).format(DAY) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
