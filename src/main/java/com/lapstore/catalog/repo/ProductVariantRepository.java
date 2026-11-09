package com.lapstore.catalog.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.catalog.domain.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_IdInAndStatusOrderByCurrentPriceAsc(Collection<Long> productIds, String status);

    List<ProductVariant> findByProduct_IdAndStatusOrderByCurrentPriceAsc(Long productId, String status);

    /** Trùng bộ (sản phẩm, màu, CPU, RAM, ổ cứng) với cấu hình khác (UNIQUE ở DB). */
    boolean existsByProduct_IdAndColor_IdAndCpuAndRamGbAndStorageGbAndIdNot(Long productId, Long colorId, String cpu,
            Integer ramGb, Integer storageGb, Long id);

    /** Có thể trả nhiều dòng nếu DB cho phép hai biến thể chỉ khác GPU; lọc GPU ở service. */
    List<ProductVariant> findByProduct_IdAndColor_IdAndCpuAndRamGbAndStorageGb(Long productId, Long colorId,
            String cpu, Integer ramGb, Integer storageGb);

    boolean existsBySku(String sku);
}
