package com.lapstore.sales.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.OrderDiscountLine;

public interface OrderDiscountLineRepository extends JpaRepository<OrderDiscountLine, Long> {
}
