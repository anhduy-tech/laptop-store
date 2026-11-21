package com.lapstore.sales.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.VoucherRedemption;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {

    long countByVoucher_IdAndCustomerPhoneAndStatusIn(Long voucherId, String customerPhone, Collection<String> statuses);

    @EntityGraph(attributePaths = "voucher")
    List<VoucherRedemption> findByOrder_IdAndStatus(Long orderId, String status);
}
