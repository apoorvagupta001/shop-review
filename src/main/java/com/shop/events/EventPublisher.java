package com.shop.events;

public interface EventPublisher {

    /**
     * Publishes a message to Kafka and returns once the broker has acknowledged it.
     * A network call: it can be slow, and it throws if Kafka cannot take the message.
     */
    void publish(String topic, String key, String payload);
}
