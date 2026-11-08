package com.lapstore.catalog.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.catalog.dto.PriceProposalRequest;
import com.lapstore.catalog.dto.PriceProposalView;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import com.lapstore.catalog.repo.ProductVariantRepository;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.staff.domain.AppUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đề xuất → duyệt giá (BR-06). Giá mới chỉ dùng để bán khi đã APPROVED và tới ngày hiệu lực.
 * price_history.status chỉ nhận PENDING | APPROVED | CANCELLED (từ chối = CANCELLED).
 */
@Service
class PriceHistoryServiceImpl implements PriceHistoryService {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final PriceHistoryRepository prices;
    private final ProductVariantRepository variants;

    @PersistenceContext
    private EntityManager em;

    PriceHistoryServiceImpl(PriceHistoryRepository prices, ProductVariantRepository variants) {
        this.prices = prices;
        this.variants = variants;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRICE_PROPOSE')")
    public void propose(PriceProposalRequest r, String username) {
        ProductVariant v = variants.findById(r.variantId())
                .filter(x -> "ACTIVE".equals(x.getStatus()))
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình (variant) có ID " + r.variantId() + "."));
        BigDecimal price = BigDecimal.valueOf(r.newPrice());
        if (price.signum() <= 0) throw new BusinessException("Giá đề xuất phải lớn hơn 0.");
        if (price.compareTo(v.getCurrentPrice()) == 0) {
            throw new BusinessException("Giá đề xuất trùng với giá hiện tại của " + v.getSku() + ".");
        }
        if (r.effectiveFrom().isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày hiệu lực không được nằm trong quá khứ.");
        }
        if (prices.existsByVariant_IdAndStatus(v.getId(), "PENDING")) {
            throw new BusinessException("Cấu hình " + v.getSku() + " đã có một đề xuất giá đang chờ duyệt.");
        }
        PriceHistory ph = new PriceHistory();
        ph.setVariant(v);
        ph.setPrice(price);
        ph.setEffectiveFrom(r.effectiveFrom().atStartOfDay(ZONE).toInstant());
        ph.setStatus("PENDING");
        ph.setReason(r.reason());
        ph.setCreatedBy(user(username));
        prices.save(ph);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRICE_APPROVE')")
    public List<PriceProposalView> pending() {
        return prices.findByStatusOrderByCreatedAtAsc("PENDING").stream()
                .map(ph -> new PriceProposalView(ph.getId(), ph.getVariant().getSku(), ph.getPrice().longValue(),
                        ph.getEffectiveFrom().atZone(ZONE).format(DMY), ph.getReason()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PRICE_APPROVE')")
    public int pendingCount() {
        return (int) prices.countByStatus("PENDING");
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRICE_APPROVE')")
    public void approve(long proposalId, String username) {
        PriceHistory ph = pendingProposal(proposalId);
        // Khóa dòng variant: hai admin duyệt hai đề xuất của cùng một máy sẽ phải xếp hàng, không chồng chéo.
        ProductVariant v = em.find(ProductVariant.class, ph.getVariant().getId(), LockModeType.PESSIMISTIC_WRITE);
        Instant now = Instant.now();
        Instant from = ph.getEffectiveFrom().isBefore(now) ? now : ph.getEffectiveFrom();

        Optional<PriceHistory> latest = prices.findFirstByVariant_IdAndStatusOrderByEffectiveFromDesc(v.getId(), "APPROVED");
        if (latest.isPresent()) {
            PriceHistory prev = latest.get();
            if (!from.isAfter(prev.getEffectiveFrom())) {
                throw new BusinessException("Khoảng hiệu lực chồng nhau với mức giá đã duyệt (hiệu lực từ "
                        + prev.getEffectiveFrom().atZone(ZONE).format(DMY) + ").");
            }
            if (prev.getEffectiveTo() == null || prev.getEffectiveTo().isAfter(from)) {
                prev.setEffectiveTo(from);   // đóng mắt xích cũ đúng lúc mức giá mới bắt đầu
            }
        }
        ph.setEffectiveFrom(from);
        ph.setStatus("APPROVED");
        ph.setApprovedBy(user(username));
        if (!from.isAfter(now)) {
            v.setCurrentPrice(ph.getPrice());   // có hiệu lực ngay; giá hẹn ngày sẽ do PriceActivationJob áp dụng
            if (latest.isEmpty() && "INACTIVE".equals(v.getStatus())) {
                v.setStatus("ACTIVE");          // giá đầu tiên của biến thể mới tạo khi nhập hàng: duyệt xong là mở bán
            }
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PRICE_APPROVE')")
    public void reject(long proposalId, String username) {
        PriceHistory ph = pendingProposal(proposalId);
        ph.setStatus("CANCELLED");
        ph.setApprovedBy(user(username));   // người quyết định (cột approved_by dùng chung cho duyệt/từ chối)
    }

    private PriceHistory pendingProposal(long id) {
        PriceHistory ph = prices.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy đề xuất " + id));
        if (!"PENDING".equals(ph.getStatus())) throw new BusinessException("Đề xuất này đã được xử lý.");
        return ph;
    }

    private AppUser user(String username) {
        List<AppUser> found = em.createQuery("select u from AppUser u where u.username = :n and u.deletedAt is null", AppUser.class)
                .setParameter("n", username).getResultList();
        if (found.isEmpty()) throw new BusinessException("Không xác định được người thao tác.");
        return found.get(0);
    }
}
