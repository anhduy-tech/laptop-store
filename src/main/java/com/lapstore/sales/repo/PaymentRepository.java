package com.lapstore.sales.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrder_IdAndStatus(Long orderId, String status);
}
