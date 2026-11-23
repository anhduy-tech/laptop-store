package com.lapstore.catalog.repo;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.catalog.domain.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

  @EntityGraph(attributePaths = "variant")
  List<PriceHistory> findByStatusOrderByCreatedAtAsc(String status);

  long countByStatus(String status);

  boolean existsByVariant_IdAndStatus(Long variantId, String status);

  /**
   * Dòng giá APPROVED có hiệu lực bắt đầu muộn nhất — mắt xích cuối của chuỗi
   * giá.
   */
  Optional<PriceHistory> findFirstByVariant_IdAndStatusOrderByEffectiveFromDesc(Long variantId, String status);

  /**
   * Các mức giá đã hết hiệu lực gần đây (mới nhất trước), để tính giá cũ và phần
   * trăm giảm.
   */
  @Query("""
      select ph from PriceHistory ph
      where ph.variant.id in :variantIds and ph.status = 'APPROVED'
        and ph.effectiveTo is not null and ph.effectiveTo <= :now and ph.effectiveTo >= :since
      order by ph.effectiveTo desc
      """)
  List<PriceHistory> findRecentlyEnded(@Param("variantIds") Collection<Long> variantIds,
      @Param("now") Instant now, @Param("since") Instant since);

  /**
   * Giá đã duyệt, đã tới ngày hiệu lực nhưng current_price của variant chưa được
   * cập nhật.
   */
  @Query("""
      select ph from PriceHistory ph join fetch ph.variant v
      where ph.status = 'APPROVED' and ph.effectiveFrom <= :now
        and (ph.effectiveTo is null or ph.effectiveTo > :now)
        and ph.price <> v.currentPrice
      """)
  List<PriceHistory> findDueForActivation(@Param("now") Instant now);

  /** Mức giá APPROVED đang hiệu lực tại thời điểm {@code now} (BR-06) — dùng để chốt giá lên đơn. */
  @Query("""
      select ph from PriceHistory ph
      where ph.variant.id = :variantId and ph.status = 'APPROVED'
        and ph.effectiveFrom <= :now and (ph.effectiveTo is null or ph.effectiveTo > :now)
      order by ph.effectiveFrom desc
      """)
  List<PriceHistory> findEffective(@Param("variantId") Long variantId, @Param("now") Instant now);

  @EntityGraph(attributePaths = "variant")
  @Query("""
      select ph from PriceHistory ph
      where ph.variant.id in :variantIds and ph.status = 'APPROVED'
        and ph.effectiveFrom <= :now and (ph.effectiveTo is null or ph.effectiveTo > :now)
      order by ph.variant.id, ph.effectiveFrom desc
      """)
  List<PriceHistory> findEffectiveByVariantIds(@Param("variantIds") Collection<Long> variantIds,
      @Param("now") Instant now);
}
