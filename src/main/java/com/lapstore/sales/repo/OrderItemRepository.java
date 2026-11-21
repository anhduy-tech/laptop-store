package com.lapstore.sales.repo;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.sales.domain.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = { "variant", "variant.product" })
    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);
}
