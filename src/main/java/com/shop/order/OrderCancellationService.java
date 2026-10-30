package com.shop.order;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.catalog.Product;
import com.shop.catalog.ProductRepository;
import com.shop.events.OrderCancelledEvent;
import com.shop.events.OutboxEvent;
import com.shop.events.OutboxRepository;

// PART H (checks 8 to 12): the new code in this pull request
@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderCancellationService(OrderRepository orderRepository, ProductRepository productRepository,
            OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Cancels a paid order, puts its stock back, and tells the other services
     * (loyalty, warehouse, email) through an OrderCancelled event on the "orders"
     * topic.
     */
    // REVIEW [Blocker] (added) A PAID order is cancelled but never refunded: the
    // customer loses the money
    // -> start a refund (an outbox event the payment service handles, with an
    // idempotency key)
    @Transactional
    public void cancel(long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Only paid orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);

        // REVIEW [Blocker] Publishing to Kafka inside the transaction: if it rolls
        // back, the event is already
        // out; if Kafka is down, the cancellation fails -> write an outbox row in this
        // same transaction;
        // the relay publishes it after commit
        UUID eventId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(eventId, "OrderCancelled",
                order.getId(), order.getCustomerId(), order.getTotalPaise());

        outboxRepository.save(new OutboxEvent(eventId, "OrderCancelled",
                String.valueOf(order.getId()), toJson(event)));

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.getSku()).orElseThrow();
            product.setStock(product.getStock() + item.getQty());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event", e);
        }
    }
}
