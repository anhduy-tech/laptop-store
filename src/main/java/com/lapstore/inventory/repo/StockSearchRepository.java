package com.lapstore.inventory.repo;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Bảng tồn kho theo (cấu hình, chi nhánh). Điều kiện ghép động, chỉ có tham số khi người dùng lọc (Oracle coi ''
 * là NULL). Đọc entity ProductVariant/Product của catalog (xem "Nợ kỹ thuật" trong CLAUDE.md).
 */
@Repository
public class StockSearchRepository {
    private static final String FROM = " from InventoryBalance b join b.variant v join v.product p"
            + " join v.color c join b.branch br";

    @PersistenceContext
    private EntityManager em;

    /** @param q null = không lọc; @param branchId null = mọi chi nhánh; @param lowOnly chỉ lấy dòng sắp hết */
    public Page<StockRowData> search(String q, Long branchId, boolean lowOnly, Pageable pageable) {
        StringBuilder where = new StringBuilder(" where p.deletedAt is null");
        if (q != null)
            where.append(" and (lower(p.name) like :pattern or lower(v.sku) like :pattern)");
        if (branchId != null)
            where.append(" and br.id = :branchId");
        if (lowOnly)
            where.append(" and (b.onHand - b.heldQty) <= coalesce(v.lowStockThreshold, 0)");

        TypedQuery<StockRowData> data = em.createQuery(
                "select new com.lapstore.inventory.repo.StockRowData(v.id, p.name, v.sku, v.variantName, c.name, "
                        + "br.id, br.name, b.onHand, b.heldQty, v.lowStockThreshold, v.status)"
                        + FROM + where + " order by p.name, v.sku, br.name",
                StockRowData.class);
        TypedQuery<Long> count = em.createQuery("select count(b)" + FROM + where, Long.class);
        if (q != null) {
            String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
            data.setParameter("pattern", pattern);
            count.setParameter("pattern", pattern);
        }
        if (branchId != null) {
            data.setParameter("branchId", branchId);
            count.setParameter("branchId", branchId);
        }
        data.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<StockRowData> rows = data.getResultList();
        return new PageImpl<>(rows, pageable, count.getSingleResult());
    }
}
