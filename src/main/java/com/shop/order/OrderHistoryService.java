package com.shop.order;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// PART D (check 4), together with OrderRepository
@Service
public class OrderHistoryService {
    // REVIEW - [Blocker] - OrderRepository is not final resulting in data getting
    // over written from one thread to another. Fix : make dependency as final and
    // use lombok constructorArgs
    private final OrderRepository orderRepository;

    public OrderHistoryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /** The "My orders" page: one line per order with its item count. */
    @Transactional(readOnly = true)
    public List<OrderSummary> summaries(String customerId) {
        List<OrderSummary> result = new ArrayList<>();
        // REVIEW - [Major] this calls N+1 queries instead of calling 2 queries. Use
        // EntityGraph or JOIN FETCH
        for (Order order : orderRepository.findByCustomerId(customerId)) {
            // REVIEW - [Major] order.getItems().size() this will loads all the rows for the
            // order and the count, Use countByItems
            result.add(
                    new OrderSummary(order.getId(), order.getItems().size(),
                            order.getTotalPaise()));
        }
        return result;
    }
}
