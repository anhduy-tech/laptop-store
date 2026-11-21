package com.lapstore.sales.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = { "variant", "variant.product" })
    List<CartItem> findByCart_IdOrderByIdAsc(Long cartId);

    Optional<CartItem> findByCart_IdAndVariant_Id(Long cartId, Long variantId);

    Optional<CartItem> findByIdAndCart_Id(Long id, Long cartId);

    void deleteByCart_Id(Long cartId);

    @Query("""
            select coalesce(sum(ci.quantity), 0)
            from CartItem ci
            join ci.cart c
            where c.cartToken = :token and c.status = 'ACTIVE'
            """)
    int sumQuantityByCartToken(@Param("token") String token);
}
