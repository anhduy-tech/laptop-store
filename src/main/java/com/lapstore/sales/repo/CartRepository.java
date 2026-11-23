package com.lapstore.sales.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCartToken(String cartToken);
}
