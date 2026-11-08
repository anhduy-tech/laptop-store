package com.lapstore.inventory.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.Branch;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.dto.StockRow;
import com.lapstore.inventory.dto.StockSerialRow;
import com.lapstore.inventory.dto.StockSerialsView;
import com.lapstore.inventory.repo.StockSearchRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
class StockQueryServiceImpl implements StockQueryService {
    private static final int MAX_SERIALS = 500;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final StockSearchRepository search;

    @PersistenceContext
    private EntityManager em;

    StockQueryServiceImpl(StockSearchRepository search) {
        this.search = search;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Page<StockRow> search(String q, Long branchId, boolean lowOnly, Pageable pageable) {
        return search.search(q, branchId, lowOnly, pageable).map(r -> new StockRow(r.variantId(), r.productName(),
                r.sku(), r.variantName(), r.colorName(), r.branchId(), r.branchName(),
                r.onHand() == null ? 0 : r.onHand(), r.held() == null ? 0 : r.held(),
                r.lowStockThreshold() == null ? 0 : r.lowStockThreshold(), r.variantStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public StockSerialsView serials(long variantId, long branchId, String status) {
        ProductVariant v = em.find(ProductVariant.class, variantId);
        Branch b = em.find(Branch.class, branchId);
        if (v == null || b == null)
            throw new NotFoundException("Không tìm thấy cấu hình hoặc chi nhánh.");

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : em.createQuery("select s.status, count(s) from SerialItem s "
                + "where s.variant.id = :v and s.branch.id = :b group by s.status order by s.status", Object[].class)
                .setParameter("v", variantId).setParameter("b", branchId).getResultList())
            counts.put((String) row[0], (Long) row[1]);

        String jpql = "select s from SerialItem s where s.variant.id = :v and s.branch.id = :b"
                + (status != null ? " and s.status = :st" : "") + " order by s.createdAt desc, s.serialNumber";
        var q = em.createQuery(jpql, SerialItem.class).setParameter("v", variantId).setParameter("b", branchId);
        if (status != null)
            q.setParameter("st", status);
        List<SerialItem> found = q.setMaxResults(MAX_SERIALS + 1).getResultList();
        boolean truncated = found.size() > MAX_SERIALS;
        List<StockSerialRow> rows = found.stream().limit(MAX_SERIALS)
                .map(s -> new StockSerialRow(s.getSerialNumber(), s.getStatus(), s.getCostPrice(),
                        s.getCreatedAt() == null ? "" : DMY.format(s.getCreatedAt())))
                .toList();
        return new StockSerialsView(variantId, branchId, v.getProduct().getName(), v.getSku(), v.getVariantName(),
                b.getName(), counts, rows, truncated);
    }
}
