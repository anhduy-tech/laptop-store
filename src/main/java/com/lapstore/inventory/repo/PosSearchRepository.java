package com.lapstore.inventory.repo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Repository;

import com.lapstore.common.dto.IdName;
import com.lapstore.inventory.dto.PosFilterOptions;
import com.lapstore.inventory.dto.PosSearchCriteria;
import com.lapstore.inventory.dto.PosSerialRow;
import com.lapstore.inventory.dto.PosVariantRow;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Tìm cấu hình + chọn serial cho popup bán quầy. Điều kiện ghép động, chỉ có tham số khi người dùng lọc (Oracle coi '' là NULL).
 * Đọc entity catalog (xem "Nợ kỹ thuật" trong CLAUDE.md).
 */
@Repository
public class PosSearchRepository {
    public static final int LIMIT = 60;

    @PersistenceContext
    private EntityManager em;

    public List<PosVariantRow> search(PosSearchCriteria c) {
        StringBuilder w = new StringBuilder(" where p.deletedAt is null and v.status = 'ACTIVE' and br.active = true");
        if (c.onlyAvailable()) w.append(" and (b.onHand - b.heldQty) > 0");
        if (c.q() != null)
            w.append(" and (lower(p.name) like :pattern or lower(v.sku) like :pattern or lower(v.variantName) like :pattern"
                    + " or exists (select 1 from SerialItem sx where sx.variant.id = v.id and sx.branch.id = br.id"
                    + " and sx.status = 'IN_STOCK' and lower(sx.serialNumber) like :pattern))");
        if (c.brandId() != null) w.append(" and p.brand.id = :brandId");
        if (c.cpu() != null) w.append(" and v.cpu = :cpu");
        if (c.gpu() != null) w.append(" and v.gpu = :gpu");
        if (c.ramGb() != null) w.append(" and v.ramGb = :ram");
        if (c.storageGb() != null) w.append(" and v.storageGb = :storage");
        if (c.minPrice() != null) w.append(" and v.currentPrice >= :minPrice");
        if (c.maxPrice() != null) w.append(" and v.currentPrice <= :maxPrice");

        TypedQuery<PosVariantRow> q = em.createQuery(
                "select new com.lapstore.inventory.dto.PosVariantRow(v.id, p.name, v.variantName, v.sku, col.name, v.cpu, v.gpu,"
                        + " v.ramGb, v.storageGb, v.currentPrice, br.id, br.name, (b.onHand - b.heldQty))"
                        + " from InventoryBalance b join b.variant v join v.product p join v.color col join b.branch br"
                        + w + " order by p.name, v.currentPrice, br.name",
                PosVariantRow.class);
        if (c.q() != null) q.setParameter("pattern", "%" + like(c.q()) + "%");
        if (c.brandId() != null) q.setParameter("brandId", c.brandId());
        if (c.cpu() != null) q.setParameter("cpu", c.cpu());
        if (c.gpu() != null) q.setParameter("gpu", c.gpu());
        if (c.ramGb() != null) q.setParameter("ram", c.ramGb());
        if (c.storageGb() != null) q.setParameter("storage", c.storageGb());
        if (c.minPrice() != null) q.setParameter("minPrice", BigDecimal.valueOf(c.minPrice()));
        if (c.maxPrice() != null) q.setParameter("maxPrice", BigDecimal.valueOf(c.maxPrice()));
        q.setMaxResults(LIMIT);
        return q.getResultList();
    }

    /** Máy IN_STOCK của một cấu hình (tại một chi nhánh nếu có), không có hold còn hạn; mới nhập lên đầu, tối đa 200. */
    public List<PosSerialRow> serials(long variantId, Long branchId, Instant now) {
        String jpql = "select new com.lapstore.inventory.dto.PosSerialRow(s.id, s.serialNumber, br.id, br.name)"
                + " from SerialItem s join s.branch br where s.variant.id = :vid and s.status = 'IN_STOCK'"
                + (branchId != null ? " and br.id = :branchId" : "")
                + " and not exists (select 1 from StockHold h where h.serial = s and h.status = 'ACTIVE' and h.expiresAt > :now)"
                + " order by s.id desc";
        TypedQuery<PosSerialRow> q = em.createQuery(jpql, PosSerialRow.class)
                .setParameter("vid", variantId).setParameter("now", now);
        if (branchId != null) q.setParameter("branchId", branchId);
        q.setMaxResults(200);
        return q.getResultList();
    }

    public PosFilterOptions options() {
        List<IdName> brands = em.createQuery("select b.id, b.name from Brand b where b.active = true order by b.name", Object[].class)
                .getResultList().stream().map(r -> new IdName((Long) r[0], (String) r[1])).toList();
        return new PosFilterOptions(brands,
                distinct("v.cpu", String.class), distinct("v.gpu", String.class),
                distinct("v.ramGb", Integer.class), distinct("v.storageGb", Integer.class));
    }

    private <T> List<T> distinct(String expr, Class<T> type) {
        return em.createQuery("select distinct " + expr + " from ProductVariant v where v.status = 'ACTIVE' and " + expr
                + " is not null order by " + expr, type).getResultList();
    }

    private static String like(String s) {
        return s.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
