package com.lapstore.sales.repo;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrder_IdAndStatus(Long orderId, String status);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.receivedBy.id = :staffId
              and p.order.shiftAssignment.id = :assignmentId
              and p.method = 'CASH'
              and p.status = 'PAID'
              and p.paidAt >= :from and p.paidAt < :to
            """)
    BigDecimal sumCashPaymentsReceivedByBetween(@Param("staffId") Long staffId, @Param("assignmentId") Long assignmentId,
            @Param("from") Instant from, @Param("to") Instant to);
}
