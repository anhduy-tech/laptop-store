package com.lapstore.inventory.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.common.exception.SerialHeldException;
import com.lapstore.common.exception.SerialNotAvailableException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.dto.HoldResult;
import com.lapstore.inventory.event.StockChanged;
import com.lapstore.inventory.repo.InventoryBalanceRepository;
import com.lapstore.inventory.repo.SerialItemRepository;
import com.lapstore.inventory.repo.StockHoldRepository;
import com.lapstore.sales.domain.CustomerOrder;
import com.lapstore.staff.domain.AppUser;

import jakarta.persistence.EntityManager;

/**
 * Lớp TRONG: mọi thay đổi DB của việc giữ máy nằm trong MỘT transaction.
 * Luôn được gọi từ HoldService khi đã giữ KeyedLock — đừng gọi trực tiếp từ nơi
 * khác.
 */
@Service
public class HoldTx {
    private static final String ACTIVE = "ACTIVE";
    private static final String EXPIRED = "EXPIRED";
    private static final String RELEASED = "RELEASED";
    private static final String CONVERTED = "CONVERTED";
    private static final String SERIAL = "SERIAL";
    private static final String QUANTITY = "QUANTITY";
    /** Đơn còn sửa được dòng hàng. */
    private static final Set<String> EDITABLE = Set.of("DRAFT", "NEW", "CONFIRMED", "PREPARING");

    private final SerialItemRepository serials;
    private final StockHoldRepository holds;
    private final InventoryBalanceRepository balances;
    private final ApplicationEventPublisher events;
    private final EntityManager em;
    private final Duration holdTime;

    HoldTx(SerialItemRepository serials, StockHoldRepository holds, InventoryBalanceRepository balances,
            ApplicationEventPublisher events, EntityManager em,
            @Value("${app.hold.counter.minutes:10}") long counterMinutes) {
        this.serials = serials;
        this.holds = holds;
        this.balances = balances;
        this.events = events;
        this.em = em;
        this.holdTime = Duration.ofMinutes(counterMinutes);
    }

    @Transactional
    public HoldResult hold(String serialNumber, long orderId, String username) {
        Instant now = Instant.now();

        SerialItem s = serials.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new BusinessException("Không tìm thấy máy có serial " + serialNumber + "."));
        if (!"IN_STOCK".equals(s.getStatus()))
            throw new SerialNotAvailableException(serialNumber, s.getStatus());

        CustomerOrder order = em.find(CustomerOrder.class, orderId);
        if (order == null)
            throw new NotFoundException("Không tìm thấy đơn " + orderId);
        if (!EDITABLE.contains(order.getStatus())) {
            throw new BusinessException("Đơn " + order.getOrderCode() + " không còn chỉnh sửa được.");
        }
        Branch fulfillBranch = order.getFulfillBranch();
        if (fulfillBranch != null && !fulfillBranch.getId().equals(s.getBranch().getId())) {
            throw new BusinessException("Máy " + serialNumber + " thuộc chi nhánh khác với đơn này.");
        }

        Long variantId = s.getVariant().getId();
        Long branchId = s.getBranch().getId();

        // 1) Đã có hold ACTIVE cho máy này?
        Optional<StockHold> current = holds.findBySerial_IdAndStatus(s.getId(), ACTIVE);
        if (current.isPresent()) {
            StockHold old = current.get();
            if (old.getExpiresAt().isAfter(now)) {
                if (old.getOrder() != null && old.getOrder().getId().equals(orderId))
                    return result(s); // quét lại
                throw new SerialHeldException(serialNumber);
            }
            // Hết hạn mà chưa có ai dọn: đánh dấu EXPIRED và trả held_qty.
            old.setStatus(EXPIRED);
            old.setReleasedAt(now);
            adjust(variantId, branchId, -old.getQuantity(), now);
            // PHẢI flush ngay: Hibernate ghi INSERT trước UPDATE, nếu không dòng ACTIVE mới
            // sẽ đụng
            // UNIQUE INDEX với dòng cũ chưa kịp chuyển sang EXPIRED.
            holds.flush();
        }

        // 2) Tạo hold SERIAL
        StockHold h = new StockHold();
        h.setOrder(order);
        h.setVariant(s.getVariant());
        h.setBranch(s.getBranch());
        h.setHoldType(SERIAL);
        h.setSerial(s);
        h.setQuantity(1);
        h.setStatus(ACTIVE);
        h.setHeldBy(em.getReference(AppUser.class, staffId(username)));
        h.setHeldAt(now);
        h.setExpiresAt(now.plus(holdTime));
        try {
            holds.saveAndFlush(h); // flush để UNIQUE nổ ngay trong khối try (2 bản ứng dụng cùng một DB)
        } catch (DataIntegrityViolationException e) {
            throw new SerialHeldException(serialNumber); // ném ra -> rollback, không dùng tiếp transaction này
        }

        // 3) held_qty: đơn online đã có hold QUANTITY thì đổi 1 đơn vị sang serial
        // (held_qty giữ nguyên),
        // ngược lại (bán quầy) tăng held_qty thêm 1.
        Optional<StockHold> qty = holds.findFirstByOrder_IdAndVariant_IdAndHoldTypeAndStatus(
                orderId, variantId, QUANTITY, ACTIVE);
        if (qty.isPresent()) {
            StockHold q = qty.get();
            int left = q.getQuantity() - 1;
            if (left <= 0) {
                q.setStatus(CONVERTED);
                q.setReleasedAt(now);
            } else {
                q.setQuantity(left);
            }
        } else if (!adjust(variantId, branchId, 1, now)) {
            throw new BusinessException("Tồn khả dụng không đủ để giữ máy " + serialNumber
                    + " (kiểm tra lại số liệu tồn kho).");
        }

        events.publishEvent(new StockChanged(variantId, branchId));
        return result(s);
    }

    @Transactional
    public void release(String serialNumber, long orderId) {
        Instant now = Instant.now();
        SerialItem s = serials.findBySerialNumber(serialNumber).orElse(null);
        if (s == null)
            return;
        Optional<StockHold> current = holds.findBySerial_IdAndStatus(s.getId(), ACTIVE);
        if (current.isEmpty())
            return; // không có gì để nhả

        StockHold h = current.get();
        if (h.getOrder() == null || !h.getOrder().getId().equals(orderId)) {
            throw new BusinessException("Máy " + serialNumber + " đang được giữ cho đơn khác, không thể nhả.");
        }
        h.setStatus(RELEASED);
        h.setReleasedAt(now);
        adjust(s.getVariant().getId(), s.getBranch().getId(), -h.getQuantity(), now);
        events.publishEvent(new StockChanged(s.getVariant().getId(), s.getBranch().getId()));
    }

    @Transactional
    public boolean renew(String serialNumber, long orderId) {
        Instant now = Instant.now();
        SerialItem s = serials.findBySerialNumber(serialNumber).orElse(null);
        if (s == null)
            return false;
        return holds.findBySerial_IdAndStatus(s.getId(), ACTIVE)
                .filter(h -> h.getOrder() != null && h.getOrder().getId().equals(orderId))
                .filter(h -> h.getExpiresAt().isAfter(now))
                .map(h -> {
                    h.setExpiresAt(now.plus(holdTime));
                    return true;
                })
                .orElse(false);
    }

    /** @return true nếu held_qty đã đổi được (1 dòng bị ảnh hưởng). */
    private boolean adjust(Long variantId, Long branchId, int delta, Instant now) {
        return balances.addHeld(variantId, branchId, delta, now) == 1;
    }

    private Long staffId(String username) {
        List<Long> ids = em.createQuery(
                "select u.id from AppUser u where u.username = :n and u.deletedAt is null", Long.class)
                .setParameter("n", username)
                .getResultList();
        if (ids.isEmpty())
            throw new BusinessException("Không xác định được nhân viên thao tác.");
        return ids.get(0);
    }

    private static HoldResult result(SerialItem s) {
        var v = s.getVariant();
        long price = v.getCurrentPrice() == null ? 0L : v.getCurrentPrice().longValue();
        return new HoldResult(s.getSerialNumber(), v.getVariantName(), price);
    }
}
