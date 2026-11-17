package com.lapstore.sales.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.VoucherCustomer;

public interface VoucherCustomerRepository extends JpaRepository<VoucherCustomer, Long> {

    List<VoucherCustomer> findByVoucher_IdAndCustomer_Id(Long voucherId, Long customerId);
}
