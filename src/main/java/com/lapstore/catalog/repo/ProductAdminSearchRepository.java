package com.lapstore.catalog.repo;

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
 * Bảng sản phẩm cho nhân viên kho: mọi trạng thái, kèm tồn kho cộng dồn các chi nhánh.
 * Điều kiện ghép động, chỉ có tham số khi người dùng lọc (Oracle coi '' là NULL).
 * Lưu ý: truy vấn con đọc entity InventoryBalance của module inventory (xem "Nợ kỹ thuật" trong CLAUDE.md).
 */
@Repository
public class ProductAdminSearchRepository {
    private static final String FROM = " from ProductVariant v join v.product p join p.brand b join v.color c";

    @PersistenceContext
    private EntityManager em;

    /** @param q null = không lọc (khớp tên sản phẩm hoặc SKU); @param brand, @param status null = tất cả */
    public Page<ProductAdminRowData> search(String q, String brand, String status, Pageable pageable) {
        StringBuilder where = new StringBuilder(" where p.deletedAt is null");
        if (q != null)
            where.append(" and (lower(p.name) like :pattern or lower(v.sku) like :pattern)");
        if (brand != null)
            where.append(" and b.name = :brand");
        if (status != null)
            where.append(" and v.status = :status");

        TypedQuery<ProductAdminRowData> data = em.createQuery(
                "select new com.lapstore.catalog.repo.ProductAdminRowData(v.id, p.id, p.name, b.name, v.sku, "
                        + "v.variantName, c.name, v.currentPrice, "
                        + "(select coalesce(sum(ib.onHand), 0) from InventoryBalance ib where ib.id.variantId = v.id), "
                        + "(select coalesce(sum(ib2.heldQty), 0) from InventoryBalance ib2 where ib2.id.variantId = v.id), "
                        + "v.lowStockThreshold, v.status, p.status)"
                        + FROM + where + " order by p.name, v.sku",
                ProductAdminRowData.class);
        TypedQuery<Long> count = em.createQuery("select count(v)" + FROM + where, Long.class);
        if (q != null) {
            String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
            data.setParameter("pattern", pattern);
            count.setParameter("pattern", pattern);
        }
        if (brand != null) {
            data.setParameter("brand", brand);
            count.setParameter("brand", brand);
        }
        if (status != null) {
            data.setParameter("status", status);
            count.setParameter("status", status);
        }
        data.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<ProductAdminRowData> rows = data.getResultList();
        return new PageImpl<>(rows, pageable, count.getSingleResult());
    }

    /** Mọi biến thể của một sản phẩm kèm tồn kho cộng dồn các chi nhánh. */
    public List<VariantRowData> variantsOf(Long productId) {
        return em.createQuery(
                "select new com.lapstore.catalog.repo.VariantRowData(v.id, v.sku, v.variantName, c.name, v.cpu, v.gpu, "
                        + "v.ramGb, v.storageGb, v.currentPrice, "
                        + "(select coalesce(sum(ib.onHand), 0) from InventoryBalance ib where ib.id.variantId = v.id), "
                        + "(select coalesce(sum(ib2.heldQty), 0) from InventoryBalance ib2 where ib2.id.variantId = v.id), "
                        + "v.lowStockThreshold, v.status) "
                        + "from ProductVariant v join v.color c where v.product.id = :pid order by v.sku",
                VariantRowData.class).setParameter("pid", productId).getResultList();
    }
}
