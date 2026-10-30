package com.shop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.shop.catalog.Product;
import com.shop.catalog.ProductRepository;
import com.shop.events.OutboxRelay;
import com.shop.loyalty.LoyaltyAccount;
import com.shop.loyalty.LoyaltyAccountRepository;
import com.shop.loyalty.LoyaltyListener;
import com.shop.order.OrderCancellationService;
import com.shop.order.OrderRepository;
import com.shop.order.OrderService;
import com.shop.order.OrderStatus;

/*
 * DAY 4, PART H CHECKS (the outbox): do not change this file.
 * Runs on its own in-memory database, separate from Day4ChecksTest.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:outbox;DB_CLOSE_DELAY=-1")
@Import({Day4ChecksTest.TestPayments.class, Day4OutboxChecksTest.TestEvents.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
class Day4OutboxChecksTest {

    @TestConfiguration
    static class TestEvents {
        @Bean
        @Primary
        RecordingEventPublisher recordingEventPublisher() {
            return new RecordingEventPublisher();
        }
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecordingEventPublisher kafka;
    @Autowired private ProductRepository productRepository;
    @Autowired private LoyaltyAccountRepository loyaltyAccountRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService;
    @Autowired private OrderCancellationService cancellationService;
    @Autowired private OutboxRelay outboxRelay;
    @Autowired private LoyaltyListener loyaltyListener;

    @BeforeEach
    void resetData() {
        jdbc.update("delete from outbox");
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        jdbc.update("delete from products");
        jdbc.update("delete from loyalty_accounts");
        productRepository.save(new Product("MOB-1", "Phone", 49_900, 5));
        loyaltyAccountRepository.save(new LoyaltyAccount("C-1", 100));
        loyaltyAccountRepository.save(new LoyaltyAccount("C-2", 100));
        kafka.reset();
    }

    @Test
    @DisplayName("8. cancel() does not publish to Kafka itself; the relay does")
    void check08_cancelLeavesPublishingToTheRelay() throws Exception {
        long orderId = paidOrder("C-1");
        cancellationService.cancel(orderId);
        assertEquals(0, kafka.sent().size(),
                "cancel() published to Kafka inside its transaction: if the transaction then rolls back, "
                        + "the event is already out. Write an outbox row instead; the relay publishes it");

        outboxRelay.publishPending();
        assertEquals(1, kafka.sent().size(), "after the relay runs, exactly one event should have been published");
    }

    @Test
    @DisplayName("9. A cancellation that rolls back publishes nothing")
    void check09_rolledBackCancellationPublishesNothing() throws Exception {
        long orderId = paidOrder("C-1");
        jdbc.update("delete from products where sku = 'MOB-1'");          // putting stock back will now fail

        assertThrows(RuntimeException.class, () -> cancellationService.cancel(orderId),
                "cancel() should fail: the product row is gone");
        outboxRelay.publishPending();

        assertEquals(0, kafka.sent().size(),
                "an OrderCancelled event went out for a cancellation that rolled back: other services now "
                        + "believe the order is cancelled");
        assertEquals(OrderStatus.PAID, orderRepository.findById(orderId).orElseThrow().getStatus(),
                "the failed cancellation should leave the order PAID");
    }

    @Test
    @DisplayName("10. If Kafka is down, cancelling still works and the relay retries until the event is sent")
    void check10_relayRetriesUntilSent() throws Exception {
        long orderId = paidOrder("C-1");
        kafka.failNextSends(1);                                          // Kafka is down for one attempt

        assertDoesNotThrow(() -> cancellationService.cancel(orderId),
                "cancel() failed because Kafka was down: cancelling must not depend on Kafka being up");
        try {
            outboxRelay.publishPending();                                // this attempt fails
        } catch (RuntimeException expected) {
            // a relay may rethrow; the next run must still retry
        }
        outboxRelay.publishPending();                                    // Kafka is back

        assertEquals(1, kafka.sent().size(),
                "the event was never published after Kafka came back: a failed send must be retried");
    }

    @Test
    @DisplayName("11. Events are keyed by order id and sent in the order they were written")
    void check11_keyedByOrderIdInOrder() throws Exception {
        long first = paidOrder("C-1");
        long second = paidOrder("C-2");
        cancellationService.cancel(first);
        cancellationService.cancel(second);
        outboxRelay.publishPending();

        List<String> keys = kafka.sent().stream().map(RecordingEventPublisher.Sent::key).toList();
        assertEquals(List.of(String.valueOf(first), String.valueOf(second)), keys,
                "Kafka keys should be the order ids, in the order the events were written; "
                        + "with no key, one order's events can land on different partitions and arrive out of order");
    }

    @Test
    @DisplayName("12. The same event delivered twice takes the loyalty points back only once")
    void check12_duplicateDeliveryHandledOnce() throws Exception {
        long orderId = paidOrder("C-1");                                 // Rs 499 -> 4 points
        cancellationService.cancel(orderId);
        outboxRelay.publishPending();
        assertFalse(kafka.sent().isEmpty(), "no event was published");
        String payload = kafka.sent().get(kafka.sent().size() - 1).payload();

        loyaltyListener.onOrderCancelled(payload);
        loyaltyListener.onOrderCancelled(payload);                       // Kafka redelivers the same message

        int points = loyaltyAccountRepository.findById("C-1").orElseThrow().getPoints();
        assertEquals(96, points,
                "the same event, delivered twice, took the points back twice (100 -> " + points + "); "
                        + "the consumer must skip an event id it has already handled");
    }

    private long paidOrder(String customerId) throws Exception {
        return orderService.placeOrder(customerId, "MOB-1", 1, "Bengaluru", null).getId();
    }
}
