package com.lapstore.sales.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findAllByOrderByIdDesc();

    /** Voucher đang ACTIVE và nằm trong thời hạn (chưa kiểm hạn mức/khách). */
    @Query("select v from Voucher v where v.status = 'ACTIVE' and v.startAt <= :now and v.endAt > :now order by v.id")
    List<Voucher> findOpen(@Param("now") Instant now);

    /**
     * Giữ chỗ một lượt (BR-07): MỘT lệnh UPDATE có điều kiện nên hai quầy cùng lấy lượt cuối thì chỉ một bên được.
     * @return 1 = giữ được; 0 = hết lượt
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Voucher v set v.reservedCount = v.reservedCount + 1, v.version = v.version + 1
             where v.id = :id and (v.totalLimit is null or v.usedCount + v.reservedCount < v.totalLimit)
            """)
    int reserve(@Param("id") Long id);

    /** Nhả một lượt đã giữ. @return 1 = thành công */
    @Modifying(flushAutomatically = true)
    @Query("update Voucher v set v.reservedCount = v.reservedCount - 1, v.version = v.version + 1 where v.id = :id and v.reservedCount > 0")
    int release(@Param("id") Long id);

    /** Chốt lượt đã giữ thành đã dùng. @return 1 = thành công */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Voucher v set v.reservedCount = v.reservedCount - 1, v.usedCount = v.usedCount + 1, v.version = v.version + 1
             where v.id = :id and v.reservedCount > 0
            """)
    int commit(@Param("id") Long id);
}
