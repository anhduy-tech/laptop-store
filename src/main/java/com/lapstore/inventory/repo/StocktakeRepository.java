package com.lapstore.inventory.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.Stocktake;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {

    @EntityGraph(attributePaths = { "branch" })
    Optional<Stocktake> findByCode(String code);

    boolean existsByBranch_IdAndStatus(Long branchId, String status);
}
