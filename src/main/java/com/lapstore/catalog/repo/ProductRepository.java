package com.lapstore.catalog.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lapstore.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "brand")
    Optional<Product> findByIdAndStatusAndDeletedAtIsNull(Long id, String status);

    /** Tên các hãng đang có sản phẩm bán, cho ô lọc ở trang danh sách. */
    @Query("""
            select distinct b.name from Product p join p.brand b
            where p.status = 'ACTIVE' and p.deletedAt is null and b.active = true
            order by b.name
            """)
    List<String> activeBrandNames();
}
