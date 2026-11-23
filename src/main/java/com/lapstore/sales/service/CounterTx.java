package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.Product;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderDiscountLine;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.OrderStatusHistory;
import com.lapstore.sales.domain.Payment;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.dto.AppliedVoucher;
import com.lapstore.sales.dto.CounterLine;
import com.lapstore.sales.dto.CounterOrderSummary;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.dto.CustomerHit;
import com.lapstore.sales.dto.DiscountDetail;
import com.lapstore.sales.dto.VoucherSuggestion;
import com.lapstore.sales.pricing.PricingEngine;
import com.lapstore.sales.pricing.PricingEngine.Applied;
import com.lapstore.sales.pricing.PricingEngine.Line;
import com.lapstore.sales.pricing.PricingEngine.Result;
import com.lapstore.sales.pricing.PricingEngine.VoucherEval;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.sales.repo.VoucherRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.domain.ShiftAssignment;
import com.lapstore.staff.service.StaffAttendanceService;

import jakarta.persistence.EntityManager;

/**
 * Phần GHI DB của bán quầy, một transaction mỗi thao tác. Luôn được gọi từ {@link CounterCheckoutServiceImpl}
 * (bean ngoài, không @Transactional) — riêng {@link #complete} chỉ gọi khi đã giữ KeyedLock của MỌI serial trong đơn.
 *
 * Giảm giá: tính lại bằng {@link PricingEngine} mỗi lần xem và chốt lại lúc complete (giá lấy từ price_history APPROVED đang
 * hiệu lực — BR-06). Voucher: giữ lượt khi áp (RESERVED), chốt khi hoàn tất (USED), nhả khi gỡ/hủy đơn (BR-07).
 */
@Service
public class CounterTx {
    private static final String SERIAL = "SERIAL";
    private static final String ACTIVE = "ACTIVE";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String WALK_IN = "Khách lẻ";
    private static final Set<String> METHODS = Set.of("CASH", "BANK_TRANSFER", "CARD_POS");

    private final EntityManager em;
    private final StockHoldRepository holds;
    private final InventoryBalanceRepository balances;
    private final PriceHistoryRepository prices;
    private final ApplicationEventPublisher events;
    private final PricingService pricing;
    private final VoucherRepository vouchers;
    private final VoucherRedemptionRepository redemptions;
    private final CustomerRepository customers;
    private final StaffAttendanceService attendance;

    CounterTx(EntityManager em, StockHoldRepository holds, InventoryBalanceRepository balances,
            PriceHistoryRepository prices, ApplicationEventPublisher events, PricingService pricing,
            VoucherRepository vouchers, VoucherRedemptionRepository redemptions, CustomerRepository customers,
            StaffAttendanceService attendance) {
        this.em = em;
        this.holds = holds;
        this.balances = balances;
        this.prices = prices;
        this.events = events;
        this.pricing = pricing;
        this.vouchers = vouchers;
        this.redemptions = redemptions;
        this.customers = customers;
        this.attendance = attendance;
    }

    // ------------------------------------------------------------------ xem

    @Transactional(readOnly = true)
    public List<CounterOrderSummary> openOrders(String username) {
        AppUser u = user(username);
        List<CounterOrderSummary> out = new ArrayList<>();
        for (CustomerOrder o : findOpen(u.getId())) {
            int n = (int) holds.countByOrder_IdAndHoldTypeAndStatus(o.getId(), SERIAL, ACTIVE);
            out.add(new CounterOrderSummary(o.getId(), o.getOrderCode(), n, o.getCustomer() == null ? null : o.getCustomerName()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public CounterOrderView view(long orderId, String username) {
        CustomerOrder o = loadOpen(orderId, user(username));
        return view(o, Instant.now());
    }

    @Transactional(readOnly = true)
    public void assertOpenOwner(long orderId, String username) {
        loadOpen(orderId, user(username));
    }

    /** Serial của các hold SERIAL đang ACTIVE của đơn — để bean ngoài lấy khóa/nhả trước khi chốt. */
    @Transactional(readOnly = true)
    public List<String> activeSerialNumbers(long orderId) {
        List<String> out = new ArrayList<>();
        for (StockHold h : holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(orderId, SERIAL, ACTIVE))
            out.add(h.getSerial().getSerialNumber());
        return out;
    }

    // ------------------------------------------------------------------ mở / hủy

    @Transactional
    public CounterOrderView open(String username) {
        AppUser u = user(username);
        if (findOpen(u.getId()).size() >= CounterCheckoutService.MAX_OPEN)
            throw new BusinessException("Bạn đang mở tối đa " + CounterCheckoutService.MAX_OPEN
                    + " đơn quầy. Hoàn tất hoặc hủy bớt một đơn trước.");
        Instant now = Instant.now();
        CustomerOrder o = new CustomerOrder();
        o.setOrderCode(newCode());
        o.setChannel("COUNTER");
        o.setStatus("DRAFT");
        o.setCustomerName(WALK_IN);
        o.setCustomerPhone("-");
        o.setFulfillmentMethod("PICKUP");
        o.setSubtotal(BigDecimal.ZERO);
        o.setDiscountTotal(BigDecimal.ZERO);
        o.setShippingFee(BigDecimal.ZERO);
        o.setTotalAmount(BigDecimal.ZERO);
        o.setPaymentStatus("UNPAID");
        o.setPlacedAt(now);
        o.setHandledBy(u);
        em.persist(o);
        history(o, null, "DRAFT", u, null, now);
        return view(o, now);
    }

    /** Các máy phải đã được nhả (bean ngoài làm trước); ở đây nhả voucher và đóng đơn. */
    @Transactional
    public void cancel(long orderId, String reason, String username) {
        Instant now = Instant.now();
        AppUser u = user(username);
        CustomerOrder o = loadOpen(orderId, u);
        if (holds.countByOrder_IdAndHoldTypeAndStatus(orderId, SERIAL, ACTIVE) > 0)
            throw new BusinessException("Đơn vẫn còn máy đang giữ, vui lòng thử lại.");
        releaseVoucher(o, now);
        o.setStatus("CANCELLED");
        String why = reason == null || reason.isBlank() ? "Nhân viên đóng đơn" : reason.trim();
        o.setCancelReason(why.length() > 255 ? why.substring(0, 255) : why);
        history(o, "DRAFT", "CANCELLED", u, o.getCancelReason(), now);
    }

    // ------------------------------------------------------------------ khách hàng

    @Transactional
    public void attachCustomer(long orderId, Long customerId, String username) {
        AppUser u = user(username);
        CustomerOrder o = loadOpen(orderId, u);
        if (customerId == null) {
            o.setCustomer(null);
            o.setCustomerName(WALK_IN);
            o.setCustomerPhone("-");
        } else {
            Customer c = customers.findById(customerId).orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng."));
            setCustomer(o, c);
        }
        dropVoucherIfBlocked(o, Instant.now());
    }

    @Transactional
    public CustomerHit quickCustomer(long orderId, String name, String phone, String username) {
        AppUser u = user(username);
        CustomerOrder o = loadOpen(orderId, u);
        String p = phone == null ? "" : phone.trim();
        if (!p.matches("[0-9+ .-]{8,20}")) throw new BusinessException("Số điện thoại không hợp lệ.");
        Customer c = customers.findByPhone(p).orElse(null);
        if (c == null) {
            if (name == null || name.isBlank()) throw new BusinessException("Nhập tên khách hàng.");
            c = new Customer();
            c.setPhone(p);
            c.setFullName(name.trim().length() > 120 ? name.trim().substring(0, 120) : name.trim());
            customers.save(c);
        }
        setCustomer(o, c);
        dropVoucherIfBlocked(o, Instant.now());
        return hit(c);
    }

    @Transactional(readOnly = true)
    public List<CustomerHit> searchCustomers(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        String pattern = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return customers.search(pattern, PageRequest.of(0, 8)).stream().map(CounterTx::hit).toList();
    }

    // ------------------------------------------------------------------ voucher

    @Transactional
    public void applyVoucher(long orderId, String code, String username) {
        Instant now = Instant.now();
        CustomerOrder o = loadOpen(orderId, user(username));
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) throw new BusinessException("Nhập mã voucher.");
        Voucher v = vouchers.findByCodeIgnoreCase(c).orElseThrow(() -> new BusinessException("Không tìm thấy voucher " + c + "."));

        VoucherRedemption cur = redemptions.findFirstByOrder_IdAndStatus(orderId, "RESERVED").orElse(null);
        if (cur != null && cur.getVoucher().getId().equals(v.getId())) return; // áp lại đúng voucher đang dùng

        List<StockHold> active = holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(orderId, SERIAL, ACTIVE);
        if (active.isEmpty()) throw new BusinessException("Đơn chưa có máy nào để áp voucher.");
        String blocked = pricing.blocker(v, o.getCustomer(), o.getCustomerPhone(), orderId, false, now);
        if (blocked != null) throw new BusinessException(blocked + ".");
        Core core = core(active, now, false);
        VoucherEval ev = PricingEngine.evalVoucher(PricingService.rule(v), core.result.subtotal() - core.result.campaignDiscount(),
                core.result.campaignDiscount());
        if (!ev.eligible()) throw new BusinessException("Chưa dùng được voucher " + v.getCode() + ": " + ev.reason() + ".");

        if (cur != null) releaseVoucher(o, now);
        if (vouchers.reserve(v.getId()) != 1) throw new BusinessException("Voucher " + v.getCode() + " đã hết lượt.");
        VoucherRedemption r = new VoucherRedemption();
        r.setVoucher(v);
        r.setOrder(o);
        r.setCustomer(o.getCustomer());
        r.setCustomerPhone(o.getCustomerPhone());
        r.setStatus("RESERVED");
        r.setDiscountAmount(BigDecimal.valueOf(ev.discount()));
        r.setReservedAt(now);
        em.persist(r);
    }

    @Transactional
    public void removeVoucher(long orderId, String username) {
        CustomerOrder o = loadOpen(orderId, user(username));
        releaseVoucher(o, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<VoucherSuggestion> voucherSuggestions(long orderId, String username) {
        Instant now = Instant.now();
        CustomerOrder o = loadOpen(orderId, user(username));
        List<StockHold> active = holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(orderId, SERIAL, ACTIVE);
        if (active.isEmpty()) return List.of();
        Core core = core(active, now, false);
        Long applied = redemptions.findFirstByOrder_IdAndStatus(orderId, "RESERVED").map(r -> r.getVoucher().getId()).orElse(null);
        return pricing.suggest(core.result.subtotal() - core.result.campaignDiscount(), core.result.campaignDiscount(),
                o.getCustomer(), o.getCustomerPhone(), orderId, applied, now);
    }

    // ------------------------------------------------------------------ chốt đơn

    @Transactional
    public String complete(long orderId, String method, Long tendered, String reference, String username) {
        Instant now = Instant.now();
        AppUser u = user(username);
        CustomerOrder o = loadOpen(orderId, u);
        ShiftAssignment activeAssignment = attendance.requireActiveAssignment(username);
        if (o.getShiftAssignment() != null && !o.getShiftAssignment().getId().equals(activeAssignment.getId())) {
            throw new BusinessException("Đơn này không thuộc ca đang chấm công của bạn.");
        }
        o.setShiftAssignment(activeAssignment);
        String pm = method == null || method.isBlank() ? "CASH" : method.trim();
        if (!METHODS.contains(pm)) throw new BusinessException("Phương thức thanh toán không hợp lệ.");

        List<StockHold> active = holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(orderId, SERIAL, ACTIVE);
        if (active.isEmpty()) throw new BusinessException("Đơn chưa có máy nào.");

        // 1) Kiểm tra: hold còn hạn, serial còn IN_STOCK, cùng một chi nhánh.
        Long branchId = active.get(0).getBranch().getId();
        Map<Long, List<StockHold>> byVariant = new LinkedHashMap<>();
        for (StockHold h : active) {
            SerialItem s = h.getSerial();
            if (!h.getExpiresAt().isAfter(now))
                throw new BusinessException("Máy " + s.getSerialNumber() + " đã hết hạn giữ, vui lòng quét lại.");
            if (!"IN_STOCK".equals(s.getStatus()))
                throw new BusinessException("Máy " + s.getSerialNumber() + " không còn ở trạng thái trong kho.");
            if (!s.getBranch().getId().equals(branchId))
                throw new BusinessException("Đơn quầy chỉ gồm máy của cùng một chi nhánh.");
            byVariant.computeIfAbsent(h.getVariant().getId(), k -> new ArrayList<>()).add(h);
        }
        o.setFulfillBranch(active.get(0).getBranch());

        // 2) Giá và giảm giá (chốt snapshot).
        Core core = core(active, now, true);
        Result res = core.result;
        VoucherRedemption red = redemptions.findFirstByOrder_IdAndStatus(orderId, "RESERVED").orElse(null);
        long voucherDiscount = 0;
        VoucherEval ev = null;
        if (red != null) {
            Voucher v = red.getVoucher();
            String blocked = pricing.blocker(v, o.getCustomer(), o.getCustomerPhone(), orderId, true, now);
            if (blocked != null) throw new BusinessException("Voucher " + v.getCode() + " không còn dùng được (" + blocked + "). Hãy gỡ voucher rồi thử lại.");
            ev = PricingEngine.evalVoucher(PricingService.rule(v), res.subtotal() - res.campaignDiscount(), res.campaignDiscount());
            if (!ev.eligible()) throw new BusinessException("Voucher " + v.getCode() + " không còn hợp lệ: " + ev.reason() + ". Hãy gỡ voucher rồi thử lại.");
            voucherDiscount = ev.discount();
        }
        long total = res.subtotal() - res.campaignDiscount() - voucherDiscount;
        if (total <= 0) throw new BusinessException("Tổng tiền sau giảm giá không hợp lệ.");

        // 3) Thanh toán.
        long tenderedAmt = total;
        long change = 0;
        if ("CASH".equals(pm)) {
            if (tendered == null) throw new BusinessException("Nhập số tiền khách đưa.");
            if (tendered < total) throw new BusinessException("Khách đưa chưa đủ: thiếu " + PricingEngine.money(total - tendered) + ".");
            tenderedAmt = tendered;
            change = tendered - total;
        }
        String ref = reference == null || reference.isBlank() ? null : reference.trim();
        if (ref != null && ref.length() > 60) throw new BusinessException("Mã giao dịch tối đa 60 ký tự.");

        // 4) order_item, serial SOLD, hold CONVERTED, trừ tồn.
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        Map<Long, OrderItem> itemOfHold = new LinkedHashMap<>();
        Map<Long, OrderItem> itemByVariant = new LinkedHashMap<>();
        for (List<StockHold> group : byVariant.values()) {
            ProductVariant v = group.get(0).getVariant();
            PriceHistory ph = effective(v.getId(), now);
            if (ph == null) throw new BusinessException("Chưa có giá được duyệt đang hiệu lực cho " + v.getVariantName() + ".");
            int qty = group.size();
            long lineDiscount = 0;
            for (StockHold h : group) lineDiscount += res.lineDiscount().getOrDefault(h.getId(), 0L);

            OrderItem item = new OrderItem();
            item.setOrder(o);
            item.setVariant(v);
            item.setQuantity(qty);
            item.setUnitPrice(ph.getPrice());
            item.setPriceHistory(ph);
            item.setPricedAt(now);
            item.setLineDiscount(BigDecimal.valueOf(lineDiscount));
            item.setLineTotal(ph.getPrice().multiply(BigDecimal.valueOf(qty)).subtract(BigDecimal.valueOf(lineDiscount)));
            em.persist(item);
            itemByVariant.put(v.getId(), item);

            int months = v.getWarrantyMonths() == null ? 12 : v.getWarrantyMonths();
            for (StockHold h : group) {
                itemOfHold.put(h.getId(), item);
                SerialItem s = h.getSerial();
                SerialStatusHistory sh = new SerialStatusHistory();
                sh.setSerial(s);
                sh.setFromStatus(s.getStatus());
                sh.setToStatus("SOLD");
                sh.setFromBranch(s.getBranch());
                sh.setRefType("ORDER");
                sh.setRefId(o.getId());
                sh.setChangedBy(u);
                sh.setChangedAt(now);
                em.persist(sh);

                s.setStatus("SOLD");
                s.setOrderItem(item);
                s.setSoldAt(now);
                s.setWarrantyStart(today);
                s.setWarrantyEnd(today.plusMonths(months));

                h.setStatus("CONVERTED");
                h.setReleasedAt(now);
                h.setOrderItem(item);
            }
            // held_qty trước, on_hand sau (điều kiện on_hand >= held_qty của addOnHand).
            if (balances.addHeld(v.getId(), branchId, -qty, now) != 1
                    || balances.addOnHand(v.getId(), branchId, -qty, now) != 1)
                throw new BusinessException("Số liệu tồn kho của " + v.getVariantName() + " không khớp, không thể chốt đơn.");
        }

        // 5) Dòng giảm giá: campaign gộp theo (campaign, order_item); voucher là một dòng cấp đơn.
        Map<String, long[]> agg = new LinkedHashMap<>();
        Map<String, Applied> sample = new LinkedHashMap<>();
        Map<String, OrderItem> itemOfKey = new LinkedHashMap<>();
        for (Applied a : res.applied()) {
            OrderItem it = itemOfHold.get(a.lineKey());
            String key = a.campaignId() + ":" + it.getId();
            agg.computeIfAbsent(key, k -> new long[1])[0] += a.amount();
            sample.putIfAbsent(key, a);
            itemOfKey.putIfAbsent(key, it);
        }
        for (var e : agg.entrySet()) {
            Applied a = sample.get(e.getKey());
            discountLine(o, itemOfKey.get(e.getKey()), "CAMPAIGN", a.campaignId(), a.name(), a.rule(), e.getValue()[0], u);
        }
        if (red != null) {
            Voucher v = red.getVoucher();
            discountLine(o, null, "VOUCHER", v.getId(), v.getCode() + " – " + v.getName(),
                    PricingEngine.describe(PricingService.rule(v)), voucherDiscount, u);
            if (vouchers.commit(v.getId()) != 1) throw new BusinessException("Không chốt được lượt voucher " + v.getCode() + ".");
            red.setStatus("USED");
            red.setUsedAt(now);
            red.setDiscountAmount(BigDecimal.valueOf(voucherDiscount));
        }

        // 6) Tổng tiền, thanh toán, trạng thái.
        o.setSubtotal(BigDecimal.valueOf(res.subtotal()));
        o.setDiscountTotal(BigDecimal.valueOf(res.campaignDiscount() + voucherDiscount));
        o.setTotalAmount(BigDecimal.valueOf(total));
        o.setPaymentStatus("PAID");
        o.setConfirmedBy(u);
        o.setConfirmedAt(now);
        o.setCompletedAt(now);
        o.setStatus("COMPLETED");
        if (o.getCustomer() != null) {
            Customer c = o.getCustomer();
            if (c.getFirstOrderAt() == null) c.setFirstOrderAt(now);
            c.setLastOrderAt(now);
        }

        Payment p = new Payment();
        p.setOrder(o);
        p.setMethod(pm);
        p.setPaymentType("FULL");
        p.setAmount(BigDecimal.valueOf(total));
        p.setTenderedAmount(BigDecimal.valueOf(tenderedAmt));
        p.setChangeAmount(BigDecimal.valueOf(change));
        p.setStatus("PAID");
        p.setReferenceCode(ref);
        p.setReceivedBy(u);
        p.setPaidAt(now);
        em.persist(p);

        history(o, "DRAFT", "COMPLETED", u, null, now);
        for (List<StockHold> group : byVariant.values())
            events.publishEvent(new StockChanged(group.get(0).getVariant().getId(), branchId));
        return o.getOrderCode();
    }

    // ------------------------------------------------------------------ helpers

    /** Kết quả tính giá cho các máy đang giữ; key của dòng = id hold. */
    private record Core(List<StockHold> holds, Map<Long, Long> unitPrice, Result result) {}

    private Core core(List<StockHold> active, Instant now, boolean strict) {
        List<Line> lines = new ArrayList<>();
        Map<Long, Long> unit = new LinkedHashMap<>();
        for (StockHold h : active) {
            ProductVariant v = h.getVariant();
            Product p = v.getProduct();
            long price = unitPrice(v, now, strict);
            unit.put(h.getId(), price);
            lines.add(new Line(h.getId(), v.getId(), p.getId(),
                    p.getBrand() == null ? 0 : p.getBrand().getId(),
                    p.getCategory() == null ? 0 : p.getCategory().getId(), price, 1));
        }
        return new Core(active, unit, PricingEngine.price(lines, pricing.runningCampaigns(now)));
    }

    private long unitPrice(ProductVariant v, Instant now, boolean strict) {
        PriceHistory ph = effective(v.getId(), now);
        if (ph != null) return ph.getPrice().longValue();
        if (strict) throw new BusinessException("Chưa có giá được duyệt đang hiệu lực cho " + v.getVariantName() + ".");
        return v.getCurrentPrice() == null ? 0 : v.getCurrentPrice().longValue();
    }

    private PriceHistory effective(Long variantId, Instant now) {
        List<PriceHistory> l = prices.findEffective(variantId, now);
        return l.isEmpty() ? null : l.get(0);
    }

    private CounterOrderView view(CustomerOrder o, Instant now) {
        List<StockHold> active = holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(o.getId(), SERIAL, ACTIVE);
        CustomerHit customer = o.getCustomer() == null ? null : hit(o.getCustomer());
        if (active.isEmpty()) {
            return new CounterOrderView(o.getId(), o.getOrderCode(), 0, 0, 0, 0, 0, 0, List.of(), customer, null, List.of());
        }
        Core core = core(active, now, false);
        Result res = core.result;
        List<CounterLine> lines = new ArrayList<>();
        long minLeft = Long.MAX_VALUE;
        for (StockHold h : active) {
            long list = core.unitPrice.get(h.getId());
            long disc = res.lineDiscount().getOrDefault(h.getId(), 0L);
            long left = Math.max(0, java.time.Duration.between(now, h.getExpiresAt()).getSeconds());
            minLeft = Math.min(minLeft, left);
            lines.add(new CounterLine(h.getSerial().getSerialNumber(), h.getVariant().getVariantName(), list, disc, list - disc, left));
        }

        List<DiscountDetail> details = new ArrayList<>();
        Map<Long, long[]> sum = new LinkedHashMap<>();
        Map<Long, Applied> first = new LinkedHashMap<>();
        for (Applied a : res.applied()) {
            sum.computeIfAbsent(a.campaignId(), k -> new long[1])[0] += a.amount();
            first.putIfAbsent(a.campaignId(), a);
        }
        for (var e : sum.entrySet()) {
            Applied a = first.get(e.getKey());
            details.add(new DiscountDetail("CAMPAIGN", a.name(), a.rule(), e.getValue()[0]));
        }

        long voucherDiscount = 0;
        AppliedVoucher av = null;
        VoucherRedemption red = redemptions.findFirstByOrder_IdAndStatus(o.getId(), "RESERVED").orElse(null);
        if (red != null) {
            Voucher v = red.getVoucher();
            VoucherEval ev = PricingEngine.evalVoucher(PricingService.rule(v), res.subtotal() - res.campaignDiscount(), res.campaignDiscount());
            voucherDiscount = ev.eligible() ? ev.discount() : 0;
            av = new AppliedVoucher(v.getCode(), v.getName(), voucherDiscount);
            details.add(new DiscountDetail("VOUCHER", v.getCode() + " – " + v.getName(),
                    ev.eligible() ? PricingEngine.describe(PricingService.rule(v)) : "Chưa đủ điều kiện: " + ev.reason(), voucherDiscount));
        }
        long discount = res.campaignDiscount() + voucherDiscount;
        return new CounterOrderView(o.getId(), o.getOrderCode(), res.subtotal(), res.campaignDiscount(), voucherDiscount, discount,
                res.subtotal() - discount, minLeft == Long.MAX_VALUE ? 0 : minLeft, lines, customer, av, details);
    }

    private void discountLine(CustomerOrder o, OrderItem item, String source, Long sourceId, String name, String rule,
                              long amount, AppUser by) {
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

    private void releaseVoucher(CustomerOrder o, Instant now) {
        redemptions.findFirstByOrder_IdAndStatus(o.getId(), "RESERVED").ifPresent(r -> {
            vouchers.release(r.getVoucher().getId());
            r.setStatus("RELEASED");
        });
    }

    /** Đổi khách có thể làm voucher không còn hợp lệ (voucher riêng, hạn mức theo khách) -> nhả. */
    private void dropVoucherIfBlocked(CustomerOrder o, Instant now) {
        redemptions.findFirstByOrder_IdAndStatus(o.getId(), "RESERVED").ifPresent(r -> {
            if (pricing.blocker(r.getVoucher(), o.getCustomer(), o.getCustomerPhone(), o.getId(), true, now) != null) {
                vouchers.release(r.getVoucher().getId());
                r.setStatus("RELEASED");
            } else {
                r.setCustomer(o.getCustomer());
                r.setCustomerPhone(o.getCustomerPhone());
            }
        });
    }

    private static void setCustomer(CustomerOrder o, Customer c) {
        o.setCustomer(c);
        o.setCustomerName(c.getFullName());
        o.setCustomerPhone(c.getPhone());
    }

    private static CustomerHit hit(Customer c) {
        return new CustomerHit(c.getId(), c.getFullName(), c.getPhone(), c.getEmail());
    }

    private List<CustomerOrder> findOpen(Long userId) {
        return em.createQuery("""
                select o from CustomerOrder o
                 where o.channel = 'COUNTER' and o.status = 'DRAFT' and o.handledBy.id = :uid
                 order by o.id
                """, CustomerOrder.class).setParameter("uid", userId).getResultList();
    }

    private CustomerOrder loadOpen(long orderId, AppUser u) {
        CustomerOrder o = em.find(CustomerOrder.class, orderId);
        if (o == null || !"COUNTER".equals(o.getChannel()) || !"DRAFT".equals(o.getStatus())
                || o.getHandledBy() == null || !o.getHandledBy().getId().equals(u.getId()))
            throw new NotFoundException("Không tìm thấy đơn quầy đang mở của bạn.");
        return o;
    }

    private void history(CustomerOrder o, String from, String to, AppUser by, String reason, Instant now) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(o);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(by);
        h.setChangedAt(now);
        h.setReason(reason);
        em.persist(h);
    }

    private AppUser user(String username) {
        List<AppUser> l = em.createQuery(
                "select u from AppUser u where u.username = :n and u.deletedAt is null", AppUser.class)
                .setParameter("n", username).getResultList();
        if (l.isEmpty()) throw new BusinessException("Không xác định được nhân viên thao tác.");
        return l.get(0);
    }

    private static String newCode() {
        return "CT" + LocalDate.now(ZONE).format(DAY) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
