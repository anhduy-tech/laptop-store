package com.lapstore.sales.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
}
