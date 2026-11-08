package com.lapstore.inventory.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.StockTransfer;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @EntityGraph(attributePaths = { "fromBranch", "toBranch" })
    Optional<StockTransfer> findByCode(String code);
}
