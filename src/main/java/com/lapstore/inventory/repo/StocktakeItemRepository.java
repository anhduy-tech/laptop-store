package com.lapstore.inventory.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.inventory.domain.StocktakeItem;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {

    List<StocktakeItem> findByStocktake_IdAndSerial_IdIn(Long stocktakeId, Collection<Long> serialIds);

    /** Dòng thiếu/dư chưa điều chỉnh, nạp sẵn máy, cấu hình và chi nhánh hiện tại của máy. */
    @Query("""
            select i from StocktakeItem i
              join fetch i.serial s
              join fetch s.variant
              join fetch s.branch
             where i.stocktake.id = :id and i.adjustedBy is null and i.result in ('MISSING', 'UNEXPECTED')
             order by s.serialNumber
            """)
    List<StocktakeItem> findAdjustable(@Param("id") Long stocktakeId);
}
