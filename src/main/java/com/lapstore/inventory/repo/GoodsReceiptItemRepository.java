package com.lapstore.inventory.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.GoodsReceiptItem;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
}
