package com.shop.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// PART H: added by the team last sprint. Sends outbox rows to Kafka.
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;

    public OutboxRelay(OutboxRepository outboxRepository, EventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs every second on every pod. Every event must reach Kafka at least once,
     * in the order it was written.
     */
    // REVIEW [Blocker] (added) @Scheduled does nothing without @EnableScheduling,
    // and the app has none: the relay
    // never runs -> add @EnableScheduling on a config class. (Left out here: the
    // checks call publishPending()
    // themselves, and a background relay would race with them.)
    // REVIEW [Major] (added) Every pod runs this: two pods read the same rows and
    // publish each event twice
    // -> lock the batch with SELECT ... FOR UPDATE SKIP LOCKED, or run a single
    // relay
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        // REVIEW [Major] findAll loaded every outbox row into memory -> unsent rows
        // only, 100 at a time, oldest first
        for (OutboxEvent event : outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                // REVIEW [Major] (corrected) Key was missing (then the outbox row id): one
                // order's events could
                // land on different partitions and arrive out of order -> key = the order id
                eventPublisher.publish("orders", event.getAggregateId(), event.getPayload());
            } catch (RuntimeException e) {
                // REVIEW [Blocker] (corrected) The row was marked published BEFORE the send and
                // the error was
                // swallowed, so a failed send was never retried -> mark only after success, and
                // stop at the
                // first failure so the events behind it keep their order
                log.warn("Publish failed for outbox event {}; will retry on the next run", event.getEventId(), e);
                return;
            }
            event.markPublished();
        }
    }
}
