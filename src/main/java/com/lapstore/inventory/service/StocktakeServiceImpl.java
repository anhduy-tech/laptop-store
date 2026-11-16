package com.lapstore.inventory.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.common.lock.KeyedLock;
import com.lapstore.inventory.dto.AdjustResult;
import com.lapstore.inventory.dto.ScanResult;
import com.lapstore.inventory.dto.StocktakeItemRow;
import com.lapstore.inventory.dto.StocktakeRow;
import com.lapstore.inventory.dto.StocktakeView;
import com.lapstore.inventory.repo.StocktakeItemRowData;
import com.lapstore.inventory.repo.StocktakeRowData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Cố ý KHÔNG có @Transactional ở cấp lớp (xem KeyedLock): các hàm đọc tự khai báo readOnly, các hàm ghi giao cho
 * StocktakeTx; điều chỉnh giữ khóa serial rồi mới gọi StocktakeTx.adjust.
 */
@Service
class StocktakeServiceImpl implements StocktakeService {
    private static final Duration WAIT = Duration.ofSeconds(5);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final String ROW_SELECT = "select new com.lapstore.inventory.repo.StocktakeRowData(t.code, b.name, "
            + "t.status, t.scope, t.startedAt, su.username, t.closedAt, cu.username, "
            + "(select count(i) from StocktakeItem i where i.stocktake.id = t.id and i.expected = true), "
            + "(select count(i) from StocktakeItem i where i.stocktake.id = t.id and i.expected = true and i.scannedAt is not null), "
            + "(select count(i) from StocktakeItem i where i.stocktake.id = t.id and i.result = 'MISSING'), "
            + "(select count(i) from StocktakeItem i where i.stocktake.id = t.id and i.result = 'UNEXPECTED'), "
            + "(select count(i) from StocktakeItem i where i.stocktake.id = t.id and i.adjustedBy is not null)) "
            + "from Stocktake t join t.branch b join t.startedBy su left join t.closedBy cu";

    private static final String ITEM_SELECT = "select new com.lapstore.inventory.repo.StocktakeItemRowData("
            + "s.serialNumber, p.name, v.sku, v.variantName, i.expected, i.result, s.status, sb.name, i.scannedAt, "
            + "i.note, ab.id) from StocktakeItem i join i.serial s join s.variant v join v.product p "
            + "join s.branch sb left join i.adjustedBy ab where i.stocktake.id = :id";

    private final KeyedLock locks;
    private final StocktakeTx tx;

    @PersistenceContext
    private EntityManager em;

    StocktakeServiceImpl(KeyedLock locks, StocktakeTx tx) {
        this.locks = locks;
        this.tx = tx;
    }

    // ------------------------------------------------------------ đọc

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Page<StocktakeRow> list(String status, Pageable pageable) {
        String where = status != null ? " where t.status = :st" : "";
        var q = em.createQuery(ROW_SELECT + where + " order by t.startedAt desc, t.id desc", StocktakeRowData.class);
        var c = em.createQuery("select count(t) from Stocktake t" + where, Long.class);
        if (status != null) {
            q.setParameter("st", status);
            c.setParameter("st", status);
        }
        List<StocktakeRow> rows = q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(StocktakeServiceImpl::toRow).toList();
        return new PageImpl<>(rows, pageable, c.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public StocktakeView get(String code, String filter, Pageable pageable) {
        StocktakeRowData head = header(code);
        Long id = idOf(code);
        String f = switch (filter == null ? "" : filter) {
            case "MATCHED" -> " and i.result = 'MATCHED'";
            case "PENDING" -> " and i.expected = true and i.scannedAt is null";
            case "UNEXPECTED" -> " and i.expected = false";
            default -> "";
        };
        String norm = f.isEmpty() ? null : filter;
        List<StocktakeItemRow> rows = em.createQuery(ITEM_SELECT + f + " order by p.name, s.serialNumber",
                StocktakeItemRowData.class).setParameter("id", id).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList().stream()
                .map(StocktakeServiceImpl::toItem).toList();
        long total = em.createQuery("select count(i) from StocktakeItem i where i.stocktake.id = :id"
                + f, Long.class).setParameter("id", id).getSingleResult();
        long adjustable = em.createQuery("select count(i) from StocktakeItem i where i.stocktake.id = :id "
                + "and i.adjustedBy is null and i.result in ('MISSING', 'UNEXPECTED')", Long.class)
                .setParameter("id", id).getSingleResult();
        return new StocktakeView(toRow(head), norm, new PageImpl<>(rows, pageable, total), adjustable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public List<StocktakeItemRow> allItems(String code) {
        Long id = idOf(code);
        return em.createQuery(ITEM_SELECT + " order by p.name, s.serialNumber", StocktakeItemRowData.class)
                .setParameter("id", id).setMaxResults(50_000).getResultList().stream()
                .map(StocktakeServiceImpl::toItem).toList();
    }

    private Long idOf(String code) {
        List<Long> ids = em.createQuery("select t.id from Stocktake t where t.code = :c", Long.class)
                .setParameter("c", code == null ? "" : code.trim()).getResultList();
        if (ids.isEmpty())
            throw new NotFoundException("Không tìm thấy phiên kiểm kê " + code);
        return ids.get(0);
    }

    private StocktakeRowData header(String code) {
        List<StocktakeRowData> list = em.createQuery(ROW_SELECT + " where t.code = :c", StocktakeRowData.class)
                .setParameter("c", code == null ? "" : code.trim()).getResultList();
        if (list.isEmpty())
            throw new NotFoundException("Không tìm thấy phiên kiểm kê " + code);
        return list.get(0);
    }

    // ------------------------------------------------------------ ghi

    @Override
    @PreAuthorize("hasAuthority('STOCK_STOCKTAKE')")
    public String start(Long branchId, String scope, String username) {
        return tx.start(branchId, scope, username);
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_STOCKTAKE')")
    public ScanResult scan(String code, List<String> serials, String username) {
        return tx.scan(code, serials == null ? List.of() : serials, username);
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_STOCKTAKE')")
    public void close(String code, String username) {
        tx.close(code, username);
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_STOCKTAKE')")
    public void cancel(String code, String username) {
        tx.cancel(code, username);
    }

    @Override
    @PreAuthorize("hasAuthority('STOCK_STOCKTAKE')")
    public AdjustResult adjust(String code, String username) {
        List<String> sns = tx.adjustableSerials(code);
        if (sns.isEmpty())
            throw new BusinessException("Không có dòng thiếu/dư nào cần điều chỉnh.");
        return locks.withLocks(sns.stream().map(s -> "serial:" + s).toList(), WAIT, () -> tx.adjust(code, username));
    }

    // ------------------------------------------------------------ ánh xạ

    private static StocktakeRow toRow(StocktakeRowData d) {
        return new StocktakeRow(d.code(), d.branchName(), d.status(), d.scope() == null ? "" : d.scope(),
                fmt(d.startedAt()), nz(d.startedBy()), fmt(d.closedAt()), nz(d.closedBy()), n(d.expected()),
                n(d.scanned()), n(d.missing()), n(d.unexpected()), n(d.adjusted()));
    }

    private static StocktakeItemRow toItem(StocktakeItemRowData d) {
        return new StocktakeItemRow(d.serialNumber(), d.productName(), d.sku(), d.variantName(),
                Boolean.TRUE.equals(d.expected()), d.result(), d.systemStatus(), d.systemBranch(), fmt(d.scannedAt()),
                d.note(), d.adjustedById() != null);
    }

    private static long n(Number x) {
        return x == null ? 0 : x.longValue();
    }

    private static String fmt(Instant t) {
        return t == null ? "" : DMY.format(t);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
