package com.shop.order;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = { "items" })
    List<Order> findByCustomerId(String customerId);

    @Query("""
                SELECT COUNT(i)
                FROM OrderItem i
                WHERE i.order.id = :orderId
            """)
    int countItemsByOrderId(Long orderId);
}
