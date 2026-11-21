package com.lapstore.sales.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
