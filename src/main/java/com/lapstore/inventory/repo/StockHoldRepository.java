package com.lapstore.inventory.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Máy đang có hold ACTIVE (kể cả hold đã quá hạn nhưng job chưa dọn). */
    boolean existsBySerial_IdAndStatus(Long serialId, String status);

    /** Id các hold ACTIVE đã quá hạn (cho job dọn, BR-04). */
    @Query("select h.id from StockHold h where h.status = 'ACTIVE' and h.expiresAt <= :now order by h.expiresAt")
    List<Long> findExpiredIds(@Param("now") Instant now);

    /** Các hold của một đơn theo loại và trạng thái; nạp sẵn serial/variant/branch để dựng màn hình. */
    @EntityGraph(attributePaths = { "serial", "variant", "branch" })
    List<StockHold> findByOrder_IdAndHoldTypeAndStatusOrderByIdAsc(Long orderId, String holdType, String status);

    /** Serial của một hold SERIAL (rỗng nếu là hold QUANTITY); tránh chạm quan hệ lazy ngoài transaction. */
    @Query("select s.serialNumber from StockHold h join h.serial s where h.id = :id")
    Optional<String> findSerialNumberByHoldId(@Param("id") Long id);

    long countByOrder_IdAndHoldTypeAndStatus(Long orderId, String holdType, String status);
}
