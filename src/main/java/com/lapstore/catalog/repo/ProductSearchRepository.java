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
 * Tìm sản phẩm theo từ khóa + hãng, phân trang. Điều kiện được ghép động và chỉ
 * có tham số khi người dùng
 * thật sự lọc: Oracle coi chuỗi rỗng là NULL nên kiểu "(:q = '' or ...)" sẽ cho
 * kết quả sai.
 */
@Repository
public class ProductSearchRepository {
    private static final String FROM = " from ProductVariant v join v.product p join p.brand b";

    @PersistenceContext
    private EntityManager em;

    /** @param q null = không lọc theo tên; @param brand null = mọi hãng */
    public Page<ProductRow> search(String q, String brand, Pageable pageable) {
        StringBuilder where = new StringBuilder(
                " where p.status = 'ACTIVE' and p.deletedAt is null and v.status = 'ACTIVE' and b.active = true");
        if (q != null)
            where.append(" and lower(p.name) like :pattern");
        if (brand != null)
            where.append(" and b.name = :brand");

        TypedQuery<ProductRow> data = em.createQuery(
                "select new com.lapstore.catalog.repo.ProductRow(p.id, p.name, b.name, p.thumbnailUrl, min(v.currentPrice))"
                        + FROM + where + " group by p.id, p.name, b.name, p.thumbnailUrl order by p.name, p.id",
                ProductRow.class);
        TypedQuery<Long> count = em.createQuery("select count(distinct p.id)" + FROM + where, Long.class);
        if (q != null) {
            String pattern = "%" + q.toLowerCase(Locale.ROOT) + "%";
            data.setParameter("pattern", pattern);
            count.setParameter("pattern", pattern);
        }
        if (brand != null) {
            data.setParameter("brand", brand);
            count.setParameter("brand", brand);
        }
        data.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<ProductRow> rows = data.getResultList();
        return new PageImpl<>(rows, pageable, count.getSingleResult());
    }
}
