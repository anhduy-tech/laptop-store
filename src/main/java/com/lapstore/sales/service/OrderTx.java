package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.Payment;
import com.lapstore.sales.domain.Voucher;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.OrderItemRepository;
import com.lapstore.sales.repo.OrderStatusHistoryRepository;
import com.lapstore.sales.repo.PaymentRepository;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.repo.AppUserRepository;

@Service
class OrderTx {
    private static final Set<String> ASSIGNABLE = Set.of("NEW", "CONFIRMED", "PREPARING");
    private static final Set<String> COMPLETABLE = Set.of("CONFIRMED", "PREPARING", "SHIPPING");
    private static final Set<String> CANCELLABLE = Set.of("DRAFT", "NEW", "CONFIRMED", "PREPARING");

    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository statusHistories;
    private final StockHoldRepository holds;
    private final SerialItemRepository serials;
    private final SerialStatusHistoryRepository serialHistories;
    private final InventoryBalanceRepository balances;
    private final VoucherRedemptionRepository redemptions;
    private final PaymentRepository payments;
    private final AppUserRepository users;
    private final ApplicationEventPublisher events;

    OrderTx(CustomerOrderRepository orders, OrderItemRepository orderItems,
            OrderStatusHistoryRepository statusHistories, StockHoldRepository holds,
            SerialItemRepository serials, SerialStatusHistoryRepository serialHistories,
            InventoryBalanceRepository balances, VoucherRedemptionRepository redemptions,
            PaymentRepository payments, AppUserRepository users, ApplicationEventPublisher events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.statusHistories = statusHistories;
        this.holds = holds;
        this.serials = serials;
        this.serialHistories = serialHistories;
        this.balances = balances;
        this.redemptions = redemptions;
        this.payments = payments;
        this.users = users;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public void validateBeforeAssign(long orderId, long itemId, String serialNumber) {
        CustomerOrder order = requireOrder(orderId);
        if (!ASSIGNABLE.contains(order.getStatus())) {
            throw new BusinessException("Đơn " + order.getOrderCode() + " không còn ở trạng thái gán serial.");
        }
        OrderItem item = orderItems.findById(itemId)
                .filter(oi -> oi.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dòng hàng " + itemId));

        SerialItem sn = serials.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new BusinessException("Không tìm thấy máy có serial " + serialNumber + "."));
        if (!sn.getVariant().getId().equals(item.getVariant().getId())) {
            throw new BusinessException("Serial " + serialNumber + " không đúng cấu hình của dòng hàng này.");
        }

        List<StockHold> activeHolds = holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE").stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null)
                .toList();
        boolean alreadyOnOrder = activeHolds.stream()
                .anyMatch(h -> h.getSerial().getSerialNumber().equals(serialNumber));
        if (alreadyOnOrder) {
            return;
        }
        long assignedCount = activeHolds.stream()
                .filter(h -> h.getOrderItem() != null
                        ? h.getOrderItem().getId().equals(itemId)
                        : h.getVariant().getId().equals(item.getVariant().getId()))
                .count();
        if (assignedCount >= item.getQuantity()) {
            throw new BusinessException("Dòng hàng này đã gán đủ " + item.getQuantity() + " serial.");
        }
    }

    @Transactional
    public void linkHoldToItem(long orderId, long itemId, String serialNumber) {
        OrderItem item = orderItems.findById(itemId)
                .filter(oi -> oi.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dòng hàng " + itemId));
        holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE").stream()
                .filter(h -> "SERIAL".equals(h.getHoldType())
                        && h.getSerial() != null
                        && h.getSerial().getSerialNumber().equals(serialNumber))
                .findFirst()
                .ifPresent(h -> h.setOrderItem(item));
    }

    @Transactional(readOnly = true)
    public List<String> heldSerials(long orderId) {
        return holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE").stream()
                .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null)
                .map(h -> h.getSerial().getSerialNumber())
                .toList();
    }

    @Transactional
    public void confirm(long orderId, String username) {
        CustomerOrder order = requireOrder(orderId);
        if (!"NEW".equals(order.getStatus())) {
            throw new BusinessException("Chỉ xác nhận được đơn ở trạng thái NEW.");
        }
        Instant now = Instant.now();
        AppUser staff = SalesSupport.requireUser(users, username);
        order.setStatus("CONFIRMED");
        order.setConfirmedBy(staff);
        order.setConfirmedAt(now);
        order.setHandledBy(staff);
        SalesSupport.recordStatus(statusHistories, order, "NEW", "CONFIRMED", staff, null, now);
    }

    @Transactional
    public void complete(long orderId, String username) {
        CustomerOrder order = requireOrder(orderId);
        String oldStatus = order.getStatus();
        if (!COMPLETABLE.contains(oldStatus)) {
            throw new BusinessException("Đơn " + order.getOrderCode() + " chưa ở trạng thái có thể hoàn tất.");
        }

        Instant now = Instant.now();
        AppUser staff = SalesSupport.requireUser(users, username);
        List<OrderItem> items = orderItems.findByOrder_IdOrderByIdAsc(orderId);
        List<StockHold> serialHolds = new ArrayList<>(
                holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE").stream()
                        .filter(h -> "SERIAL".equals(h.getHoldType()) && h.getSerial() != null)
                        .toList());

        for (OrderItem oi : items) {
            List<StockHold> matched = new ArrayList<>();
            for (StockHold h : serialHolds) {
                if (matched.size() >= oi.getQuantity()) break;
                boolean sameItem = h.getOrderItem() != null
                        ? h.getOrderItem().getId().equals(oi.getId())
                        : h.getVariant().getId().equals(oi.getVariant().getId());
                if (sameItem) {
                    matched.add(h);
                }
            }
            if (matched.size() < oi.getQuantity()) {
                throw new BusinessException("Vui lòng gán đủ serial cho sản phẩm "
                        + SalesSupport.variantDisplayName(oi.getVariant()) + " trước khi hoàn tất.");
            }
            serialHolds.removeAll(matched);
            for (StockHold h : matched) {
                if (h.getExpiresAt().isBefore(now)) {
                    throw new BusinessException("Serial " + h.getSerial().getSerialNumber()
                            + " đã hết thời gian giữ, vui lòng gán lại.");
                }
                SalesSupport.finalizeHeldSerial(h, oi, order, staff, balances, serialHistories, events, now);
            }
        }

        for (VoucherRedemption vr : redemptions.findByOrder_IdAndStatus(orderId, "RESERVED")) {
            vr.setStatus("USED");
            vr.setUsedAt(now);
            Voucher v = vr.getVoucher();
            v.setReservedCount(Math.max(0, v.getReservedCount() - 1));
            v.setUsedCount(v.getUsedCount() + 1);
        }

        if (!payments.existsByOrder_IdAndStatus(orderId, "PAID") && order.getTotalAmount().signum() > 0) {
            Payment p = new Payment();
            p.setOrder(order);
            p.setMethod("DELIVERY".equals(order.getFulfillmentMethod()) ? "COD" : "CASH");
            p.setPaymentType("FULL");
            p.setAmount(order.getTotalAmount());
            p.setTenderedAmount(order.getTotalAmount());
            p.setChangeAmount(BigDecimal.ZERO);
            p.setStatus("PAID");
            p.setReceivedBy(staff);
            p.setPaidAt(now);
            payments.save(p);
        }

        order.setStatus("COMPLETED");
        order.setPaymentStatus("PAID");
        order.setCompletedAt(now);
        order.setHandledBy(staff);
        SalesSupport.recordStatus(statusHistories, order, oldStatus, "COMPLETED", staff, null, now);
    }

    @Transactional
    public void cancel(long orderId, String reason, String username) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do hủy.");
        }
        CustomerOrder order = requireOrder(orderId);
        String oldStatus = order.getStatus();
        if (!CANCELLABLE.contains(oldStatus)) {
            throw new BusinessException("Đơn " + order.getOrderCode() + " không thể hủy ở trạng thái " + oldStatus + ".");
        }

        Instant now = Instant.now();
        AppUser staff = SalesSupport.requireUser(users, username);

        for (StockHold h : holds.findByOrder_IdAndStatusOrderByIdAsc(orderId, "ACTIVE")) {
            h.setStatus("RELEASED");
            h.setReleasedAt(now);
            Long variantId = h.getVariant().getId();
            Long branchId = h.getBranch().getId();
            balances.addHeld(variantId, branchId, -h.getQuantity(), now);
            events.publishEvent(new StockChanged(variantId, branchId));
        }

        for (VoucherRedemption vr : redemptions.findByOrder_IdAndStatus(orderId, "RESERVED")) {
            vr.setStatus("RELEASED");
            Voucher v = vr.getVoucher();
            v.setReservedCount(Math.max(0, v.getReservedCount() - 1));
        }

        order.setStatus("CANCELLED");
        order.setCancelReason(reason.trim());
        order.setHandledBy(staff);
        SalesSupport.recordStatus(statusHistories, order, oldStatus, "CANCELLED", staff, reason.trim(), now);
    }

    private CustomerOrder requireOrder(long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn " + orderId));
    }
}
