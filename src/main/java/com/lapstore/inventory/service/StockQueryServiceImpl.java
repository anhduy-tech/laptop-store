package com.lapstore.inventory.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import com.lapstore.inventory.dto.TransferItemRow;
import com.lapstore.inventory.dto.TransferRow;
import com.lapstore.inventory.dto.TransferView;
import com.lapstore.inventory.repo.StockTransferItemRepository;
import com.lapstore.inventory.repo.StockTransferRepository;
import com.lapstore.inventory.repo.TransferRowData;
import com.lapstore.inventory.repo.StockSearchRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
class StockQueryServiceImpl implements StockQueryService {
    private static final int MAX_SERIALS = 500;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final StockSearchRepository search;
    private final StockTransferRepository transfers;
    private final StockTransferItemRepository transferItems;

    @PersistenceContext
    private EntityManager em;

    StockQueryServiceImpl(StockSearchRepository search, StockTransferRepository transfers,
            StockTransferItemRepository transferItems) {
        this.search = search;
        this.transfers = transfers;
        this.transferItems = transferItems;
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

    private static final String TRANSFER_SELECT = "select new com.lapstore.inventory.repo.TransferRowData(t.code, "
            + "fb.name, tb.name, t.status, (select count(i) from StockTransferItem i where i.transfer.id = t.id), "
            + "t.shippedAt, su.username, t.receivedAt, ru.username) from StockTransfer t join t.fromBranch fb "
            + "join t.toBranch tb left join t.shippedBy su left join t.receivedBy ru";

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Page<TransferRow> transfers(String status, Pageable pageable) {
        String where = status != null ? " where t.status = :st" : "";
        var q = em.createQuery(TRANSFER_SELECT + where + " order by t.shippedAt desc, t.id desc", TransferRowData.class);
        var c = em.createQuery("select count(t) from StockTransfer t" + where, Long.class);
        if (status != null) {
            q.setParameter("st", status);
            c.setParameter("st", status);
        }
        List<TransferRow> rows = q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(StockQueryServiceImpl::toRow).toList();
        return new PageImpl<>(rows, pageable, c.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public TransferView transfer(String code) {
        var t = transfers.findByCode(code).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chuyển " + code));
        TransferRowData data = em.createQuery(TRANSFER_SELECT + " where t.id = :id", TransferRowData.class)
                .setParameter("id", t.getId()).getSingleResult();
        List<TransferItemRow> items = transferItems.findWithSerial(t.getId()).stream().map(i -> new TransferItemRow(
                i.getSerial().getSerialNumber(), i.getSerial().getVariant().getProduct().getName(),
                i.getSerial().getVariant().getSku(), i.getSerial().getVariant().getVariantName(),
                i.getSerial().getStatus())).toList();
        return new TransferView(toRow(data), t.getNote(), items);
    }

    private static TransferRow toRow(TransferRowData d) {
        return new TransferRow(d.code(), d.fromBranch(), d.toBranch(), d.status(),
                d.itemCount() == null ? 0 : d.itemCount().longValue(), fmt(d.shippedAt()), nz(d.shippedBy()),
                fmt(d.receivedAt()), nz(d.receivedBy()));
    }

    private static String fmt(Instant t) {
        return t == null ? "" : DMY.format(t);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
