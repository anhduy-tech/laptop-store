package com.lapstore.inventory.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.GoodsReceipt;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
}
