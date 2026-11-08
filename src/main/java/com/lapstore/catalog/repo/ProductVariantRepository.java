package com.lapstore.catalog.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.catalog.domain.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_IdInAndStatusOrderByCurrentPriceAsc(Collection<Long> productIds, String status);

    List<ProductVariant> findByProduct_IdAndStatusOrderByCurrentPriceAsc(Long productId, String status);
}
