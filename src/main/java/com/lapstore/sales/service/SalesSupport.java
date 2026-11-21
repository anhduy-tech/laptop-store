package com.lapstore.sales.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialStatusHistoryRepository;
import com.lapstore.sales.domain.Customer;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.OrderStatusHistory;
import com.lapstore.sales.repo.CustomerOrderRepository;
import com.lapstore.sales.repo.CustomerRepository;
import com.lapstore.sales.repo.OrderStatusHistoryRepository;
import com.lapstore.staff.domain.AppUser;
import com.lapstore.staff.repo.AppUserRepository;

import org.springframework.context.ApplicationEventPublisher;

/** Shared helpers for sales services (order codes, pricing, serial finalization). */
final class SalesSupport {
    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM")
            .withZone(ZoneId.systemDefault());

    private SalesSupport() {}

    static String variantDisplayName(ProductVariant v) {
        return v.getProduct().getName() + " · " + v.getVariantName();
    }

    static String generateOrderCode(String prefix, CustomerOrderRepository orders, Instant now) {
        String ym = YYMM.format(now);
        for (int i = 0; i < 20; i++) {
            int n = ThreadLocalRandom.current().nextInt(1000, 10000);
            String code = prefix + ym + "-" + n;
            if (!orders.existsByOrderCode(code)) {
                return code;
            }
        }
        return prefix + ym + "-" + (now.toEpochMilli() % 100000);
    }

    static AppUser requireUser(AppUserRepository users, String username) {
        return users.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản nhân viên: " + username));
    }

    static PriceHistory requireApprovedPrice(PriceHistoryRepository prices, Long variantId) {
        return prices.findFirstByVariant_IdAndStatusOrderByEffectiveFromDesc(variantId, "APPROVED")
                .orElseThrow(() -> new BusinessException("Sản phẩm chưa có giá niêm yết hợp lệ."));
    }

    static Customer upsertCustomer(CustomerRepository customers, String phone, String fullName, String email, Instant now) {
        return customers.findByPhone(phone).map(c -> {
            if (fullName != null && !fullName.isBlank()) c.setFullName(fullName);
            if (email != null && !email.isBlank()) c.setEmail(email);
            if (c.getFirstOrderAt() == null) c.setFirstOrderAt(now);
            c.setLastOrderAt(now);
            return c;
        }).orElseGet(() -> {
            Customer c = new Customer();
            c.setPhone(phone);
            c.setFullName(fullName);
            c.setEmail(email);
            c.setFirstOrderAt(now);
            c.setLastOrderAt(now);
            return customers.save(c);
        });
    }

    static void recordStatus(OrderStatusHistoryRepository historyRepo, CustomerOrder order,
                             String fromStatus, String toStatus, AppUser actor, String reason, Instant now) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setFromStatus(fromStatus);
        h.setToStatus(toStatus);
        h.setChangedBy(actor);
        h.setChangedAt(now);
        h.setReason(reason);
        historyRepo.save(h);
    }

    /** Chuyển một hold SERIAL ACTIVE thành SOLD, trừ held_qty và on_hand, ghi lịch sử serial. */
    static void finalizeHeldSerial(StockHold hold, OrderItem item, CustomerOrder order, AppUser actor,
                                   InventoryBalanceRepository balances,
                                   SerialStatusHistoryRepository serialHistories,
                                   ApplicationEventPublisher events, Instant now) {
        SerialItem s = hold.getSerial();
        if (s == null || !"IN_STOCK".equals(s.getStatus())) {
            throw new BusinessException("Serial không còn ở trạng thái sẵn sàng bán.");
        }
        Long variantId = s.getVariant().getId();
        Long branchId = s.getBranch().getId();

        hold.setStatus("CONVERTED");
        hold.setOrderItem(item);
        hold.setReleasedAt(now);

        LocalDate today = LocalDate.now();
        int warrantyMonths = s.getVariant().getWarrantyMonths() != null ? s.getVariant().getWarrantyMonths() : 12;
        s.setStatus("SOLD");
        s.setOrderItem(item);
        s.setSoldAt(now);
        s.setWarrantyStart(today);
        s.setWarrantyEnd(today.plusMonths(warrantyMonths));

        SerialStatusHistory sh = new SerialStatusHistory();
        sh.setSerial(s);
        sh.setFromStatus("IN_STOCK");
        sh.setToStatus("SOLD");
        sh.setFromBranch(s.getBranch());
        sh.setRefType("ORDER");
        sh.setRefId(order.getId());
        sh.setChangedBy(actor);
        sh.setChangedAt(now);
        serialHistories.save(sh);

        // Giảm held_qty trước khi giảm on_hand để luôn thỏa CHECK (held_qty <= on_hand)
        if (balances.addHeld(variantId, branchId, -1, now) == 0
                || balances.addOnHand(variantId, branchId, -1, now) == 0) {
            throw new BusinessException("Số liệu tồn kho không khớp khi xuất serial " + s.getSerialNumber() + ".");
        }
        events.publishEvent(new StockChanged(variantId, branchId));
    }
}
