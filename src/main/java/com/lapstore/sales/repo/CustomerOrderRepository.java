package com.lapstore.sales.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    boolean existsByOrderCode(String orderCode);

    Optional<CustomerOrder> findByOrderCodeIgnoreCaseAndCustomerPhone(String orderCode, String customerPhone);

    Optional<CustomerOrder> findFirstByChannelAndStatusAndHandledBy_UsernameOrderByIdDesc(
            String channel, String status, String username);

    @Query("""
            select o from CustomerOrder o
            where (:status = '' or o.status = :status)
              and (:kw = ''
                   or lower(o.orderCode) like lower(concat('%', :kw, '%'))
                   or o.customerPhone like concat('%', :kw, '%')
                   or lower(o.customerName) like lower(concat('%', :kw, '%')))
            order by o.placedAt desc, o.id desc
            """)
    List<CustomerOrder> search(@Param("status") String status, @Param("kw") String kw);
}
