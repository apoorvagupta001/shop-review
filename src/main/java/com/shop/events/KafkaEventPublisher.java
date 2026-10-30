package com.shop.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka in this exercise. In production: kafkaTemplate.send(topic, key, payload).get(5, SECONDS). */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    @Override
    public void publish(String topic, String key, String payload) {
        log.info("Published to {} with key {}: {}", topic, key, payload);
    }
}
