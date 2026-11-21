package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.repo.BranchRepository;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.Payment;
import com.lapstore.sales.dto.CounterLine;
import com.lapstore.sales.dto.CounterOrderView;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.OrderItemRepository;
import com.lapstore.sales.repo.OrderStatusHistoryRepository;
import com.lapstore.sales.repo.PaymentRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.repo.AppUserRepository;

@Service
class CounterTx {
    private static final String CHANNEL_COUNTER = "COUNTER";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository statusHistories;
    private final CustomerRepository customers;
    private final PaymentRepository payments;
    private final StockHoldRepository holds;
    private final InventoryBalanceRepository balances;
    private final SerialStatusHistoryRepository serialHistories;
    private final PriceHistoryRepository prices;
    private final AppUserRepository users;
    private final BranchRepository branches;
    private final ApplicationEventPublisher events;

    CounterTx(CustomerOrderRepository orders, OrderItemRepository orderItems,
              OrderStatusHistoryRepository statusHistories, CustomerRepository customers,
              PaymentRepository payments, StockHoldRepository holds,
              InventoryBalanceRepository balances, SerialStatusHistoryRepository serialHistories,
              PriceHistoryRepository prices, AppUserRepository users,
              BranchRepository branches, ApplicationEventPublisher events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.statusHistories = statusHistories;
        this.customers = customers;
        this.payments = payments;
        this.holds = holds;
        this.balances = balances;
        this.serialHistories = serialHistories;
        this.prices = prices;
        this.users = users;
        this.branches = branches;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Optional<CounterOrderView> current(String username) {
        return orders.findFirstByChannelAndStatusAndHandledBy_UsernameOrderByIdDesc(
                CHANNEL_COUNTER, STATUS_DRAFT, username)
                .map(this::toView);
    }

    @Transactional
    public CounterOrderView open(String username) {
        Optional<CustomerOrder> existing = orders.findFirstByChannelAndStatusAndHandledBy_UsernameOrderByIdDesc(
                CHANNEL_COUNTER, STATUS_DRAFT, username);
        if (existing.isPresent()) {
            return toView(existing.get());
        }

        Instant now = Instant.now();
        AppUser staff = SalesSupport.requireUser(users, username);
        Branch branch = staff.getBranch();
        if (branch == null) {
            branch = branches.findByActiveTrueOrderByNameAsc().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("Hệ thống chưa có chi nhánh hoạt động."));
        }

        CustomerOrder order = new CustomerOrder();
        order.setOrderCode(SalesSupport.generateOrderCode("CT", orders, now));
        order.setChannel(CHANNEL_COUNTER);
        order.setStatus(STATUS_DRAFT);
        order.setCustomerName("Khách lẻ");
        order.setCustomerPhone("0000000000");
        order.setFulfillmentMethod("PICKUP");
        order.setFulfillBranch(branch);
        order.setHandledBy(staff);
        order.setSubtotal(BigDecimal.ZERO);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaymentStatus("UNPAID");
        order.setPlacedAt(now);
        orders.save(order);

        SalesSupport.recordStatus(statusHistories, order, null, STATUS_DRAFT, staff, null, now);
        return new CounterOrderView(order.getId(), order.getOrderCode(), 0L, List.of());
    }

    @Transactional(readOnly = true)
    public List<String> heldSerials(long orderId) {
        return holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, STATUS_ACTIVE).stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null)
                .map(h -> h.getSerial().getSerialNumber())
                .toList();
    }

    @Transactional
    public String complete(long orderId, String customerName, String phone, String username) {
        Instant now = Instant.now();
        AppUser staff = SalesSupport.requireUser(users, username);
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn " + orderId));
        if (!CHANNEL_COUNTER.equals(order.getChannel()) || !STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException("Đơn " + order.getOrderCode() + " không ở trạng thái đơn quầy đang mở.");
        }

        List<StockHold> activeHolds = holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, STATUS_ACTIVE).stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()))
                .toList();
        if (activeHolds.isEmpty()) {
            throw new BusinessException("Đơn chưa có máy nào.");
        }
        for (StockHold h : activeHolds) {
            if (h.getExpiresAt().isBefore(now)) {
                throw new BusinessException("Serial " + h.getSerial().getSerialNumber()
                        + " đã hết thời gian giữ, vui lòng quét lại.");
            }
        }

        if (phone != null && !phone.isBlank()) {
            String cleanPhone = phone.trim();
            String cleanName = (customerName != null && !customerName.isBlank()) ? customerName.trim() : "Khách lẻ";
            Customer c = SalesSupport.upsertCustomer(customers, cleanPhone, cleanName, null, now);
            order.setCustomer(c);
            order.setCustomerPhone(cleanPhone);
            order.setCustomerName(cleanName);
        } else if (customerName != null && !customerName.isBlank()) {
            order.setCustomerName(customerName.trim());
        }

        Map<Long, List<StockHold>> byVariant = activeHolds.stream()
                .collect(Collectors.groupingBy(h -> h.getVariant().getId()));

        BigDecimal total = BigDecimal.ZERO;
        for (List<StockHold> group : byVariant.values()) {
            ProductVariant variant = group.get(0).getVariant();
            PriceHistory ph = SalesSupport.requireApprovedPrice(prices, variant.getId());
            BigDecimal unitPrice = variant.getCurrentPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(group.size()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setVariant(variant);
            item.setQuantity(group.size());
            item.setUnitPrice(unitPrice);
            item.setPriceHistory(ph);
            item.setPricedAt(now);
            item.setLineDiscount(BigDecimal.ZERO);
            item.setLineTotal(lineTotal);
            orderItems.save(item);

            for (StockHold h : group) {
                SalesSupport.finalizeHeldSerial(h, item, order, staff, balances, serialHistories, events, now);
            }
            total = total.add(lineTotal);
        }

        order.setSubtotal(total);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(total);
        order.setStatus("COMPLETED");
        order.setPaymentStatus("PAID");
        order.setConfirmedBy(staff);
        order.setConfirmedAt(now);
        order.setCompletedAt(now);

        SalesSupport.recordStatus(statusHistories, order, STATUS_DRAFT, "COMPLETED", staff, null, now);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod("CASH");
        payment.setPaymentType("FULL");
        payment.setAmount(total);
        payment.setTenderedAmount(total);
        payment.setChangeAmount(BigDecimal.ZERO);
        payment.setStatus("PAID");
        payment.setReceivedBy(staff);
        payment.setPaidAt(now);
        payments.save(payment);

        return order.getOrderCode();
    }

    private CounterOrderView toView(CustomerOrder order) {
        Instant now = Instant.now();
        List<CounterLine> lines = holds.findByOrder_IdAndStatusOrderByIdAsc(order.getId(), STATUS_ACTIVE).stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null && h.getExpiresAt().isAfter(now))
                .map(h -> new CounterLine(
                        h.getSerial().getSerialNumber(),
                        SalesSupport.variantDisplayName(h.getVariant()),
                        h.getVariant().getCurrentPrice().longValue()))
                .toList();
        long total = lines.stream().mapToLong(CounterLine::price).sum();
        return new CounterOrderView(order.getId(), order.getOrderCode(), total, lines);
    }
}
