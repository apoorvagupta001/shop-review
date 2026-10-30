package com.shop.loyalty;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

// PART H (check 12): the loyalty service's Kafka consumer
@Component
public class LoyaltyListener {

    private final LoyaltyAccountRepository accounts;
    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper objectMapper;

    public LoyaltyListener(LoyaltyAccountRepository accounts, ProcessedEventRepository processedEvents,
            ObjectMapper objectMapper) {
        this.accounts = accounts;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    /**
     * Called for every message on the "orders" topic (a @KafkaListener in
     * production).
     * Takes back the points earned on a cancelled order: 1 point per Rs 100.
     * Kafka delivers at least once, so the same message can arrive more than once.
     */
    @Transactional
    public void onOrderCancelled(String payload) throws Exception {

        Map<?, ?> event = objectMapper.readValue(payload, Map.class);
        // REVIEW [Blocker] No idempotency: Kafka delivers at least once, so a
        // redelivered event took the points
        // back twice -> record each event id in the same transaction, and skip ids
        // already seen
        UUID eventId = UUID.fromString((String) event.get("eventId"));
        if (processedEvents.existsById(eventId)) {
            return; // a duplicate delivery: already handled
        }
        processedEvents.save(new ProcessedEvent(eventId));

        String customerId = (String) event.get("customerId");
        long totalPaise = ((Number) event.get("totalPaise")).longValue();

        LoyaltyAccount account = accounts.findById(customerId).orElseThrow();
        // REVIEW [Minor] (added) Points could go below zero -> floor at 0
        account.setPoints(Math.max(0, account.getPoints() - (int) (totalPaise / 10_000)));
    }
}
