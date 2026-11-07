package com.lapstore.inventory.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.StockHold;

public interface StockHoldRepository extends JpaRepository<StockHold, Long> {

    /**
     * Hold ACTIVE của một serial. Chỉ có tối đa một nhờ UNIQUE INDEX
     * uq_stock_hold_active_serial.
     */
    Optional<StockHold> findBySerial_IdAndStatus(Long serialId, String status);

    /**
     * Hold theo số lượng (đơn online) của một đơn + variant, để đổi thành hold theo
     * serial khi quét máy.
     */
    Optional<StockHold> findFirstByOrder_IdAndVariant_IdAndHoldTypeAndStatus(
            Long orderId, Long variantId, String holdType, String status);
}
