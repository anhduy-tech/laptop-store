package com.lapstore.sales.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.sales.domain.OrderItem;
import com.lapstore.sales.domain.OrderStatusHistory;
import com.lapstore.sales.domain.Payment;
import com.lapstore.sales.domain.VoucherRedemption;
import com.lapstore.sales.repo.VoucherRedemptionRepository;
import com.lapstore.sales.repo.VoucherRepository;
import com.lapstore.staff.domain.AppUser;

import jakarta.persistence.EntityManager;

/**
 * Phần GHI DB xử lý đơn online của nhân viên (xác nhận, kiểm gán serial, hoàn tất, hủy). Luôn được gọi từ
 * {@link OrderServiceImpl} (bean ngoài, không @Transactional); {@link #complete} và {@link #cancel} chỉ gọi khi bean ngoài đã
 * giữ KeyedLock của MỌI serial đang giữ cho đơn. Vòng đời: NEW -> CONFIRMED -> COMPLETED, hoặc CANCELLED từ NEW/CONFIRMED.
 */
@Service
public class OrderTx {
    private static final String ACTIVE = "ACTIVE";
    private static final String SERIAL = "SERIAL";
    private static final String QUANTITY = "QUANTITY";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> OPEN = Set.of("NEW", "CONFIRMED");

    private final EntityManager em;
    private final StockHoldRepository holds;
    private final InventoryBalanceRepository balances;
    private final VoucherRepository vouchers;
    private final VoucherRedemptionRepository redemptions;
    private final ApplicationEventPublisher events;
    private final Duration confirmedHold;

    OrderTx(EntityManager em, StockHoldRepository holds, InventoryBalanceRepository balances, VoucherRepository vouchers,
            VoucherRedemptionRepository redemptions, ApplicationEventPublisher events,
            @Value("${app.hold.confirmed.minutes:4320}") long confirmedMinutes) {
        this.em = em;
        this.holds = holds;
        this.balances = balances;
        this.vouchers = vouchers;
        this.redemptions = redemptions;
        this.events = events;
        this.confirmedHold = Duration.ofMinutes(confirmedMinutes);
    }

    /** Serial của các hold SERIAL đang ACTIVE của đơn — để bean ngoài lấy khóa trước khi hoàn tất/hủy. */
    @Transactional(readOnly = true)
    public List<String> activeSerialNumbers(long orderId) {
        List<String> out = new ArrayList<>();
        for (StockHold h : holds.findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(orderId, SERIAL, ACTIVE))
            out.add(h.getSerial().getSerialNumber());
        return out;
    }

    // ------------------------------------------------------------------ xác nhận

    /** NEW -> CONFIRMED. Hold số lượng đã hết hạn (khách đặt lâu) thì giữ lại nếu còn hàng, không thì từ chối. */
    @Transactional
    public void confirm(long orderId, String username) {
        Instant now = Instant.now();
        AppUser u = user(username);
        CustomerOrder o = load(orderId);
        if (!"NEW".equals(o.getStatus()))
            throw new BusinessException("Chỉ xác nhận được đơn ở trạng thái NEW.");
        for (OrderItem it : items(orderId)) {
            Long variantId = it.getVariant().getId();
            long serialHeld = serialHolds(orderId, variantId).size();
            int need = (int) (it.getQuantity() - serialHeld);
            if (need <= 0)
                continue;
            StockHold q = holds.findFirstByOrder_IdAndVariant_IdAndHoldTypeAndStatus(orderId, variantId, QUANTITY, ACTIVE)
                    .orElse(null);
            if (q != null && !q.getExpiresAt().isAfter(now)) { // hết hạn nhưng job chưa dọn
                q.setStatus("EXPIRED");
                q.setReleasedAt(now);
                balances.addHeld(variantId, q.getBranch().getId(), -q.getQuantity(), now);
                holds.flush();
                q = null;
            }
            if (q == null) {
                if (balances.addHeld(variantId, o.getFulfillBranch().getId(), need, now) != 1)
                    throw new BusinessException("Không thể xác nhận: " + it.getVariant().getVariantName()
                            + " không còn đủ hàng tại chi nhánh xử lý. Hãy hủy đơn và báo khách.");
                q = new StockHold();
                q.setOrder(o);
                q.setOrderItem(it);
                q.setVariant(it.getVariant());
                q.setBranch(o.getFulfillBranch());
                q.setHoldType(QUANTITY);
                q.setQuantity(need);
                q.setStatus(ACTIVE);
                q.setHeldAt(now);
                em.persist(q);
            }
            q.setExpiresAt(now.plus(confirmedHold));
            events.publishEvent(new StockChanged(variantId, o.getFulfillBranch().getId()));
        }
        o.setStatus("CONFIRMED");
        o.setConfirmedBy(u);
        o.setConfirmedAt(now);
        o.setHandledBy(u);
        history(o, "NEW", "CONFIRMED", u, null, now);
    }

    // ------------------------------------------------------------------ gán serial

    /** Kiểm tra trước khi giữ máy: đúng dòng, đúng cấu hình, còn chỗ, máy đang IN_STOCK. Việc giữ do StockHoldService làm. */
    @Transactional(readOnly = true)
    public void checkAssign(long orderId, long itemId, String serialNumber) {
        CustomerOrder o = load(orderId);
        if (!OPEN.contains(o.getStatus()))
            throw new BusinessException("Đơn " + o.getOrderCode() + " không còn gán serial được.");
        OrderItem it = em.find(OrderItem.class, itemId);
        if (it == null || !it.getOrder().getId().equals(orderId))
            throw new NotFoundException("Không tìm thấy dòng hàng " + itemId + " trong đơn.");
        List<SerialItem> found = em.createQuery("select s from SerialItem s where s.serialNumber = :sn", SerialItem.class)
                .setParameter("sn", serialNumber).setMaxResults(1).getResultList();
        if (found.isEmpty())
            throw new BusinessException("Không tìm thấy máy có serial " + serialNumber + ".");
        SerialItem s = found.get(0);
        if (!s.getVariant().getId().equals(it.getVariant().getId()))
            throw new BusinessException("Máy " + serialNumber + " không đúng cấu hình của dòng hàng này ("
                    + it.getVariant().getVariantName() + ").");
        boolean already = serialHolds(orderId, it.getVariant().getId()).stream()
                .anyMatch(h -> h.getSerial().getId().equals(s.getId()));
        if (!already && serialHolds(orderId, it.getVariant().getId()).size() >= it.getQuantity())
            throw new BusinessException("Dòng này đã gán đủ " + it.getQuantity() + " máy.");
    }

    // ------------------------------------------------------------------ hoàn tất

    @Transactional
    public String complete(long orderId, String username) {
        Instant now = Instant.now();
        AppUser u = user(username);
        CustomerOrder o = load(orderId);
        if (!"CONFIRMED".equals(o.getStatus()))
            throw new BusinessException("Chỉ hoàn tất được đơn đã xác nhận.");
        Long branchId = o.getFulfillBranch().getId();
        LocalDate today = LocalDate.ofInstant(now, ZONE);

        for (OrderItem it : items(orderId)) {
            Long variantId = it.getVariant().getId();
            List<StockHold> sh = serialHolds(orderId, variantId);
            if (sh.size() != it.getQuantity())
                throw new BusinessException(it.getVariant().getVariantName() + ": mới gán " + sh.size() + "/"
                        + it.getQuantity() + " máy. Hãy gán đủ serial trước khi hoàn tất.");
            int months = it.getVariant().getWarrantyMonths() == null ? 12 : it.getVariant().getWarrantyMonths();
            for (StockHold h : sh) {
                SerialItem s = h.getSerial();
                if (!h.getExpiresAt().isAfter(now))
                    throw new BusinessException("Máy " + s.getSerialNumber() + " đã hết hạn giữ, vui lòng gán lại.");
                if (!"IN_STOCK".equals(s.getStatus()))
                    throw new BusinessException("Máy " + s.getSerialNumber() + " không còn ở trạng thái trong kho.");
                if (!s.getBranch().getId().equals(branchId))
                    throw new BusinessException("Máy " + s.getSerialNumber() + " không thuộc chi nhánh xử lý đơn.");

                SerialStatusHistory hist = new SerialStatusHistory();
                hist.setSerial(s);
                hist.setFromStatus(s.getStatus());
                hist.setToStatus("SOLD");
                hist.setFromBranch(s.getBranch());
                hist.setRefType("ORDER");
                hist.setRefId(o.getId());
                hist.setChangedBy(u);
                hist.setChangedAt(now);
                em.persist(hist);

                s.setStatus("SOLD");
                s.setOrderItem(it);
                s.setSoldAt(now);
                s.setWarrantyStart(today);
                s.setWarrantyEnd(today.plusMonths(months));

                h.setStatus("CONVERTED");
                h.setReleasedAt(now);
                h.setOrderItem(it);
            }
            // held_qty trước, on_hand sau (điều kiện on_hand >= held_qty của addOnHand).
            int qty = it.getQuantity();
            if (balances.addHeld(variantId, branchId, -qty, now) != 1 || balances.addOnHand(variantId, branchId, -qty, now) != 1)
                throw new BusinessException("Số liệu tồn kho của " + it.getVariant().getVariantName()
                        + " không khớp, không thể hoàn tất.");
            events.publishEvent(new StockChanged(variantId, branchId));
        }

        VoucherRedemption red = redemptions.findFirstByOrder_IdAndStatus(orderId, "RESERVED").orElse(null);
        if (red != null) {
            if (vouchers.commit(red.getVoucher().getId()) != 1)
                throw new BusinessException("Không chốt được lượt voucher " + red.getVoucher().getCode() + ".");
            red.setStatus("USED");
            red.setUsedAt(now);
        }

        Payment p = new Payment();
        p.setOrder(o);
        p.setMethod("DELIVERY".equals(o.getFulfillmentMethod()) ? "COD" : "CASH");
        p.setPaymentType("FULL");
        p.setAmount(o.getTotalAmount());
        p.setStatus("PAID");
        p.setReceivedBy(u);
        p.setPaidAt(now);
        em.persist(p);

        o.setPaymentStatus("PAID");
        o.setCompletedAt(now);
        o.setStatus("COMPLETED");
        history(o, "CONFIRMED", "COMPLETED", u, null, now);
        return o.getOrderCode();
    }

    // ------------------------------------------------------------------ hủy

    /** Nhả mọi hold ACTIVE (số lượng và serial) của đơn, trả lượt voucher, đóng đơn (BR-11). */
    @Transactional
    public void cancel(long orderId, String reason, String username) {
        Instant now = Instant.now();
        AppUser u = user(username);
        CustomerOrder o = load(orderId);
        if (!OPEN.contains(o.getStatus()))
            throw new BusinessException("Chỉ hủy được đơn chưa hoàn tất.");
        List<StockHold> active = em.createQuery("select h from StockHold h join fetch h.variant join fetch h.branch "
                + "where h.order.id = :id and h.status = 'ACTIVE'", StockHold.class).setParameter("id", orderId)
                .getResultList();
        for (StockHold h : active) {
            h.setStatus("RELEASED");
            h.setReleasedAt(now);
            balances.addHeld(h.getVariant().getId(), h.getBranch().getId(), -h.getQuantity(), now);
            events.publishEvent(new StockChanged(h.getVariant().getId(), h.getBranch().getId()));
        }
        redemptions.findFirstByOrder_IdAndStatus(orderId, "RESERVED").ifPresent(r -> {
            vouchers.release(r.getVoucher().getId());
            r.setStatus("RELEASED");
        });
        String why = reason == null || reason.isBlank() ? "Nhân viên hủy đơn" : reason.trim();
        o.setCancelReason(why.length() > 255 ? why.substring(0, 255) : why);
        String from = o.getStatus();
        o.setStatus("CANCELLED");
        history(o, from, "CANCELLED", u, o.getCancelReason(), now);
    }

    // ------------------------------------------------------------------ helpers

    private CustomerOrder load(long id) {
        CustomerOrder o = em.find(CustomerOrder.class, id);
        if (o == null || !"ONLINE".equals(o.getChannel()))
            throw new NotFoundException("Không tìm thấy đơn online " + id);
        if (o.getFulfillBranch() == null)
            throw new BusinessException("Đơn " + o.getOrderCode() + " chưa có chi nhánh xử lý.");
        return o;
    }

    private List<OrderItem> items(long orderId) {
        return em.createQuery("select i from OrderItem i join fetch i.variant where i.order.id = :id order by i.id",
                OrderItem.class).setParameter("id", orderId).getResultList();
    }

    private List<StockHold> serialHolds(long orderId, Long variantId) {
        return em.createQuery("select h from StockHold h join fetch h.serial where h.order.id = :o "
                + "and h.variant.id = :v and h.holdType = 'SERIAL' and h.status = 'ACTIVE'", StockHold.class)
                .setParameter("o", orderId).setParameter("v", variantId).getResultList();
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
        List<AppUser> l = em.createQuery("select u from AppUser u where u.username = :n and u.deletedAt is null",
                AppUser.class).setParameter("n", username).getResultList();
        if (l.isEmpty())
            throw new BusinessException("Không xác định được nhân viên thao tác.");
        return l.get(0);
    }
}
