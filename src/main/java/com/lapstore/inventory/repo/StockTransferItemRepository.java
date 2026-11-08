package com.lapstore.inventory.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.inventory.domain.StockTransferItem;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, Long> {

    @Query("""
            select i from StockTransferItem i
              join fetch i.serial s
              join fetch s.variant
             where i.transfer.id = :transferId
            """)
    List<StockTransferItem> findWithSerial(@Param("transferId") Long transferId);
}
