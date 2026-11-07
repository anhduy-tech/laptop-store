package com.lapstore.inventory.repo;

import java.util.List;

import com.lapstore.inventory.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrueOrderByNameAsc();
}
