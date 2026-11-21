package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.InventoryBalance;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.BranchRepository;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.Cart;
import com.lapstore.sales.domain.CartItem;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderDiscountLine;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.dto.CheckoutRequest;
import com.lapstore.sales.repo.CartItemRepository;
import com.lapstore.sales.repo.CartRepository;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.OrderDiscountLineRepository;
import com.lapstore.sales.repo.OrderItemRepository;
import com.lapstore.sales.repo.OrderStatusHistoryRepository;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.sales.repo.VoucherRepository;

@Service
class OnlineOrderServiceImpl implements OnlineOrderService {
    private static final Duration ONLINE_HOLD_TTL = Duration.ofHours(24);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final CustomerRepository customers;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository statusHistories;
    private final PriceHistoryRepository prices;
    private final BranchRepository branches;
    private final InventoryBalanceRepository balances;
    private final StockHoldRepository holds;
    private final VoucherRepository vouchers;
    private final VoucherRedemptionRepository redemptions;
    private final OrderDiscountLineRepository discountLines;
    private final ApplicationEventPublisher events;

    OnlineOrderServiceImpl(CartRepository carts, CartItemRepository cartItems,
                           CustomerRepository customers, CustomerOrderRepository orders,
                           OrderItemRepository orderItems, OrderStatusHistoryRepository statusHistories,
                           PriceHistoryRepository prices, BranchRepository branches,
                           InventoryBalanceRepository balances, StockHoldRepository holds,
                           VoucherRepository vouchers, VoucherRedemptionRepository redemptions,
                           OrderDiscountLineRepository discountLines, ApplicationEventPublisher events) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.customers = customers;
        this.orders = orders;
        this.orderItems = orderItems;
        this.statusHistories = statusHistories;
        this.prices = prices;
        this.branches = branches;
        this.balances = balances;
        this.holds = holds;
        this.vouchers = vouchers;
        this.redemptions = redemptions;
        this.discountLines = discountLines;
        this.events = events;
    }

    @Override
    @Transactional
    public String placeOrder(String cartToken, CheckoutRequest request) {
        Cart cart = carts.findByCartToken(cartToken)
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống."));
        List<CartItem> items = cartItems.findByCart_IdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống.");
        }

        String method = request.receiveMethod();
        if (!"PICKUP".equals(method) && !"DELIVERY".equals(method)) {
            throw new BusinessException("Hình thức nhận hàng không hợp lệ.");
        }
        if ("DELIVERY".equals(method) && (request.address() == null || request.address().isBlank())) {
            throw new BusinessException("Vui lòng nhập địa chỉ giao hàng.");
        }

        Instant now = Instant.now();
        Branch fulfillBranch = pickFulfillBranch(items);

        // Giữ tồn kho theo số lượng tại chi nhánh đã chọn
        for (CartItem ci : items) {
            int updated = balances.addHeld(ci.getVariant().getId(), fulfillBranch.getId(), ci.getQuantity(), now);
            if (updated == 0) {
                throw new BusinessException("Sản phẩm " + SalesSupport.variantDisplayName(ci.getVariant())
                        + " không đủ tồn kho khả dụng.");
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : items) {
            BigDecimal line = ci.getVariant().getCurrentPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(line);
        }

        Voucher appliedVoucher = null;
        BigDecimal discountTotal = BigDecimal.ZERO;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            appliedVoucher = validateVoucher(request.voucherCode().trim(), request.phone(), subtotal, now);
            discountTotal = calculateDiscount(appliedVoucher, subtotal);
            appliedVoucher.setReservedCount(appliedVoucher.getReservedCount() + 1);
        }

        BigDecimal totalAmount = subtotal.subtract(discountTotal).max(BigDecimal.ZERO);
        Customer customer = SalesSupport.upsertCustomer(
                customers, request.phone(), request.fullName(), request.email(), now);

        CustomerOrder order = new CustomerOrder();
        order.setOrderCode(SalesSupport.generateOrderCode("LS", orders, now));
        order.setChannel("ONLINE");
        order.setStatus("NEW");
        order.setCustomer(customer);
        order.setCustomerName(request.fullName());
        order.setCustomerPhone(request.phone());
        order.setCustomerEmail(request.email());
        order.setFulfillmentMethod(method);
        order.setShipAddressLine("DELIVERY".equals(method) ? request.address() : null);
        order.setFulfillBranch(fulfillBranch);
        order.setSubtotal(subtotal);
        order.setDiscountTotal(discountTotal);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus("UNPAID");
        order.setPlacedAt(now);
        orders.save(order);

        for (CartItem ci : items) {
            ProductVariant variant = ci.getVariant();
            PriceHistory ph = SalesSupport.requireApprovedPrice(prices, variant.getId());
            BigDecimal lineTotal = variant.getCurrentPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setVariant(variant);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(variant.getCurrentPrice());
            oi.setPriceHistory(ph);
            oi.setPricedAt(now);
            oi.setLineDiscount(BigDecimal.ZERO);
            oi.setLineTotal(lineTotal);
            orderItems.save(oi);

            StockHold hold = new StockHold();
            hold.setOrder(order);
            hold.setOrderItem(oi);
            hold.setVariant(variant);
            hold.setBranch(fulfillBranch);
            hold.setHoldType("QUANTITY");
            hold.setQuantity(ci.getQuantity());
            hold.setStatus("ACTIVE");
            hold.setHeldAt(now);
            hold.setExpiresAt(now.plus(ONLINE_HOLD_TTL));
            holds.save(hold);

            events.publishEvent(new StockChanged(variant.getId(), fulfillBranch.getId()));
        }

        if (appliedVoucher != null) {
            VoucherRedemption vr = new VoucherRedemption();
            vr.setVoucher(appliedVoucher);
            vr.setOrder(order);
            vr.setCustomer(customer);
            vr.setCustomerPhone(request.phone());
            vr.setStatus("RESERVED");
            vr.setDiscountAmount(discountTotal);
            vr.setReservedAt(now);
            vr.setExpiresAt(now.plus(ONLINE_HOLD_TTL));
            redemptions.save(vr);

            OrderDiscountLine dl = new OrderDiscountLine();
            dl.setOrder(order);
            dl.setSourceType("VOUCHER");
            dl.setSourceId(appliedVoucher.getId());
            dl.setNameSnapshot(appliedVoucher.getCode());
            dl.setAmount(discountTotal);
            dl.setReason("Áp dụng voucher " + appliedVoucher.getCode());
            discountLines.save(dl);
        }

        SalesSupport.recordStatus(statusHistories, order, null, "NEW", null, null, now);
        cartItems.deleteByCart_Id(cart.getId());

        return order.getOrderCode();
    }

    private Branch pickFulfillBranch(List<CartItem> items) {
        List<Branch> activeBranches = branches.findByActiveTrueOrderByNameAsc();
        Map<Long, Map<Long, Integer>> availByVariantBranch = items.stream()
                .collect(Collectors.toMap(
                        ci -> ci.getVariant().getId(),
                        ci -> balances.findById_VariantId(ci.getVariant().getId()).stream()
                                .collect(Collectors.toMap(
                                        b -> b.getId().branchId(),
                                        b -> b.getOnHand() - b.getHeldQty())),
                        (a, b) -> a));

        for (Branch branch : activeBranches) {
            boolean enough = items.stream().allMatch(ci -> {
                Map<Long, Integer> byBranch = availByVariantBranch.getOrDefault(ci.getVariant().getId(), Map.of());
                return byBranch.getOrDefault(branch.getId(), 0) >= ci.getQuantity();
            });
            if (enough) {
                return branch;
            }
        }
        throw new BusinessException("Hiện chưa có chi nhánh nào đủ tồn kho khả dụng cho toàn bộ giỏ hàng.");
    }

    private Voucher validateVoucher(String code, String phone, BigDecimal subtotal, Instant now) {
        Voucher v = vouchers.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Mã voucher không tồn tại."));
        if (!"ACTIVE".equals(v.getStatus())) {
            throw new BusinessException("Voucher " + v.getCode() + " hiện không hoạt động.");
        }
        if (now.isBefore(v.getStartAt()) || now.isAfter(v.getEndAt())) {
            throw new BusinessException("Voucher " + v.getCode() + " chưa đến hạn hoặc đã hết hạn.");
        }
        if (subtotal.compareTo(v.getMinOrder()) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher " + v.getCode() + ".");
        }
        if (v.getTotalLimit() != null && v.getUsedCount() + v.getReservedCount() >= v.getTotalLimit()) {
            throw new BusinessException("Voucher " + v.getCode() + " đã hết lượt sử dụng.");
        }
        if (v.getPerCustomerLimit() != null) {
            long used = redemptions.countByVoucher_IdAndCustomerPhoneAndStatusIn(
                    v.getId(), phone, List.of("RESERVED", "USED"));
            if (used >= v.getPerCustomerLimit()) {
                throw new BusinessException("Số điện thoại này đã hết lượt sử dụng voucher " + v.getCode() + ".");
            }
        }
        return v;
    }

    private static BigDecimal calculateDiscount(Voucher v, BigDecimal subtotal) {
        BigDecimal discount;
        if ("PERCENT".equals(v.getDiscountType())) {
            discount = subtotal.multiply(v.getDiscountValue()).divide(HUNDRED, 0, RoundingMode.HALF_UP);
            if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) {
                discount = v.getMaxDiscount();
            }
        } else {
            discount = v.getDiscountValue();
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }
}
