package com.lapstore.inventory.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByActiveTrueOrderByNameAsc();
}
