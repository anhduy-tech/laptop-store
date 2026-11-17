package com.lapstore.sales.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    /** Tìm theo tên (không phân biệt hoa thường) hoặc một phần số điện thoại; pattern dạng %abc% đã thoát ký tự `!`. */
    @Query("""
            select c from Customer c
             where lower(c.fullName) like :pattern escape '!' or c.phone like :pattern escape '!'
             order by c.lastOrderAt desc nulls last, c.fullName
            """)
    List<Customer> search(@Param("pattern") String pattern, Pageable pageable);
}
