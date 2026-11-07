package com.lapstore.inventory.repo;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.inventory.domain.InventoryBalance;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, InventoryBalance.Id> {

    /**
     * Đổi held_qty bằng MỘT lệnh UPDATE: vừa kiểm điều kiện vừa ghi, không "đọc rồi mới ghi".
     * Hai luồng cùng giữ chiếc cuối cùng -> Oracle xếp hàng, luồng sau kiểm lại WHERE và nhận 0 dòng.
     * Điều kiện giữ 0 <= held_qty <= on_hand (khớp CHECK của bảng).
     *
     * @return số dòng bị ảnh hưởng: 1 = thành công, 0 = không đủ tồn (hoặc chưa có dòng tồn)
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update InventoryBalance b
               set b.heldQty = b.heldQty + :delta,
                   b.version = b.version + 1,
                   b.updatedAt = :now
             where b.id.variantId = :variantId
               and b.id.branchId = :branchId
               and b.heldQty + :delta >= 0
               and b.heldQty + :delta <= b.onHand
            """)
    int addHeld(@Param("variantId") Long variantId, @Param("branchId") Long branchId,
                @Param("delta") int delta, @Param("now") Instant now);
}
