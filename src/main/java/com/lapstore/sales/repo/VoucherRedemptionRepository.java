package com.lapstore.sales.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.VoucherRedemption;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {

    @EntityGraph(attributePaths = "voucher")
    Optional<VoucherRedemption> findFirstByOrder_IdAndStatus(Long orderId, String status);

    @EntityGraph(attributePaths = "voucher")
    List<VoucherRedemption> findByOrder_IdAndStatus(Long orderId, String status);

    /** Số lượt đã giữ/dùng của một số điện thoại với voucher (không tính đơn đang xét). */
    long countByVoucher_IdAndCustomerPhoneAndStatusInAndOrder_IdNot(Long voucherId, String phone,
            Collection<String> statuses, Long orderId);

    long countByVoucher_IdAndCustomerPhoneAndStatusIn(Long voucherId, String customerPhone, Collection<String> statuses);
}
