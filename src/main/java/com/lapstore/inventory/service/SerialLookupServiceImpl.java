package com.lapstore.inventory.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lapstore.catalog.domain.ProductVariant;
import com.lapstore.common.exception.NotFoundException;
import com.lapstore.inventory.domain.GoodsReceipt;
import com.lapstore.inventory.domain.GoodsReceiptItem;
import com.lapstore.inventory.domain.SerialItem;
import com.lapstore.inventory.domain.SerialStatusHistory;
import com.lapstore.inventory.domain.StockHold;
import com.lapstore.inventory.domain.StockTransfer;
import com.lapstore.inventory.domain.StockTransferItem;
import com.lapstore.inventory.domain.Stocktake;
import com.lapstore.inventory.domain.StocktakeItem;
import com.lapstore.inventory.dto.SerialDetailView;
import com.lapstore.inventory.dto.SerialDetailView.HistoryRow;
import com.lapstore.inventory.dto.SerialDetailView.HoldInfo;
import com.lapstore.inventory.dto.SerialDetailView.StocktakeRef;
import com.lapstore.inventory.dto.SerialDetailView.TransferRef;
import com.lapstore.inventory.dto.SerialSearchRow;
import com.lapstore.inventory.repo.SerialRowData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
class SerialLookupServiceImpl implements SerialLookupService {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PersistenceContext
    private EntityManager em;

    // ------------------------------------------------------------ tìm

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Page<SerialSearchRow> search(String q, String status, Pageable pageable) {
        String kw = q == null ? "" : q.trim();
        if (kw.length() < MIN_QUERY)
            return new PageImpl<>(List.of(), pageable, 0);
        String lower = kw.toLowerCase(Locale.ROOT);
        String where = " where lower(s.serialNumber) like :pat escape '!'" + (status != null ? " and s.status = :st" : "");
        String from = " from SerialItem s join s.variant v join v.product p join v.color c join s.branch br";
        var data = em.createQuery("select new com.lapstore.inventory.repo.SerialRowData(s.serialNumber, p.name, v.sku, "
                + "v.variantName, c.name, br.name, s.status, s.createdAt)" + from + where
                + " order by case when lower(s.serialNumber) = :exact then 0 else 1 end, s.serialNumber",
                SerialRowData.class);
        var count = em.createQuery("select count(s)" + from + where, Long.class);
        String pattern = "%" + escapeLike(lower) + "%";
        data.setParameter("pat", pattern).setParameter("exact", lower);
        count.setParameter("pat", pattern);
        if (status != null) {
            data.setParameter("st", status);
            count.setParameter("st", status);
        }
        List<SerialSearchRow> rows = data.setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList().stream()
                .map(d -> new SerialSearchRow(d.serialNumber(), d.productName(), d.sku(), d.variantName(),
                        d.colorName(), d.branchName(), d.status(), fmt(d.createdAt())))
                .toList();
        return new PageImpl<>(rows, pageable, count.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public Optional<String> findExact(String q) {
        String kw = q == null ? "" : q.trim();
        if (kw.isEmpty())
            return Optional.empty();
        return em.createQuery("select s.serialNumber from SerialItem s where lower(s.serialNumber) = :sn", String.class)
                .setParameter("sn", kw.toLowerCase(Locale.ROOT)).setMaxResults(1).getResultList().stream().findFirst();
    }

    // ------------------------------------------------------------ hồ sơ máy

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PORTAL_WAREHOUSE')")
    public SerialDetailView detail(String serialNumber) {
        String kw = serialNumber == null ? "" : serialNumber.trim().toLowerCase(Locale.ROOT);
        List<SerialItem> found = em.createQuery("select s from SerialItem s where lower(s.serialNumber) = :sn",
                SerialItem.class).setParameter("sn", kw).setMaxResults(1).getResultList();
        if (found.isEmpty())
            throw new NotFoundException("Không tìm thấy serial " + serialNumber);
        SerialItem s = found.get(0);
        ProductVariant v = s.getVariant();

        GoodsReceiptItem gri = s.getGoodsReceiptItem();
        GoodsReceipt gr = gri == null ? null : gri.getReceipt();

        List<HistoryRow> history = new ArrayList<>();
        for (SerialStatusHistory h : em.createQuery("select h from SerialStatusHistory h left join fetch h.fromBranch "
                + "left join fetch h.toBranch left join fetch h.changedBy where h.serial.id = :id "
                + "order by h.changedAt, h.id", SerialStatusHistory.class).setParameter("id", s.getId())
                .getResultList()) {
            history.add(new HistoryRow(fmt(h.getChangedAt()), h.getFromStatus() == null ? "" : h.getFromStatus(),
                    h.getToStatus(), h.getFromBranch() == null ? "" : h.getFromBranch().getName(),
                    h.getToBranch() == null ? "" : h.getToBranch().getName(), h.getRefType(),
                    refCode(h.getRefType(), h.getRefId()),
                    h.getChangedBy() == null ? "" : h.getChangedBy().getUsername()));
        }

        List<StocktakeRef> stocktakes = em.createQuery("select i from StocktakeItem i join fetch i.stocktake st "
                + "join fetch st.branch where i.serial.id = :id order by st.startedAt desc", StocktakeItem.class)
                .setParameter("id", s.getId()).getResultList().stream()
                .map(i -> new StocktakeRef(i.getStocktake().getCode(), i.getStocktake().getBranch().getName(),
                        i.getResult() == null ? "" : i.getResult(), fmt(i.getScannedAt())))
                .toList();

        List<TransferRef> transfers = em.createQuery("select ti from StockTransferItem ti join fetch ti.transfer t "
                + "join fetch t.fromBranch join fetch t.toBranch where ti.serial.id = :id order by t.shippedAt desc",
                StockTransferItem.class).setParameter("id", s.getId()).getResultList().stream()
                .map(ti -> new TransferRef(ti.getTransfer().getCode(), ti.getTransfer().getStatus(),
                        ti.getTransfer().getFromBranch().getName(), ti.getTransfer().getToBranch().getName()))
                .toList();

        HoldInfo hold = em.createQuery("select h from StockHold h left join fetch h.order left join fetch h.heldBy "
                + "where h.serial.id = :id and h.status = 'ACTIVE' order by h.heldAt desc", StockHold.class)
                .setParameter("id", s.getId()).setMaxResults(1).getResultList().stream().findFirst()
                .map(h -> new HoldInfo(h.getHoldType(), h.getOrder() == null ? "" : h.getOrder().getOrderCode(),
                        h.getHeldBy() == null ? "" : h.getHeldBy().getUsername(), fmt(h.getHeldAt()),
                        fmt(h.getExpiresAt())))
                .orElse(null);

        return new SerialDetailView(s.getSerialNumber(), s.getStatus(), v.getProduct().getId(),
                v.getProduct().getName(), v.getProduct().getBrand().getName(), v.getId(), v.getSku(),
                v.getVariantName(), v.getColor().getName(), v.getCpu(), v.getGpu(), v.getRamGb(), v.getStorageGb(),
                s.getBranch().getName(), canSeeCost() ? s.getCostPrice() : null,
                gr == null ? "" : gr.getCode(), gr == null || gr.getSupplier() == null ? "" : gr.getSupplier().getName(),
                gr == null ? fmt(s.getCreatedAt()) : fmt(gr.getReceivedAt()),
                s.getOrderItem() == null ? "" : s.getOrderItem().getOrder().getOrderCode(), fmt(s.getSoldAt()),
                s.getWarrantyEnd() == null ? "" : DAY.format(s.getWarrantyEnd()), hold, history, stocktakes, transfers);
    }

    // ------------------------------------------------------------ tiện ích

    /** Mã phiếu của một lần đổi trạng thái (nhập, chuyển, kiểm kê); loại khác trả null. */
    private String refCode(String refType, Long refId) {
        if (refType == null || refId == null)
            return null;
        return switch (refType) {
            case "RECEIPT" -> codeOf(GoodsReceipt.class, refId);
            case "TRANSFER" -> codeOf(StockTransfer.class, refId);
            case "STOCKTAKE" -> codeOf(Stocktake.class, refId);
            default -> null;
        };
    }

    private String codeOf(Class<?> type, Long id) {
        List<String> r = em.createQuery("select e.code from " + type.getSimpleName() + " e where e.id = :id",
                String.class).setParameter("id", id).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    private static boolean canSeeCost() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(x -> "COST_VIEW".equals(x.getAuthority()));
    }

    /** Thoát ký tự đặc biệt của LIKE, dùng với escape '!' (tránh phụ thuộc cách xử lý dấu gạch chéo ngược). */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String fmt(Instant t) {
        return t == null ? "" : DMY.format(t);
    }
}
