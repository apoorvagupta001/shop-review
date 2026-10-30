package com.shop;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.shop.events.EventPublisher;

/** Test stand-in for Kafka. Records what was published, and can pretend Kafka is down. */
public class RecordingEventPublisher implements EventPublisher {

    public record Sent(String topic, String key, String payload) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private final AtomicInteger failuresLeft = new AtomicInteger();

    @Override
    public void publish(String topic, String key, String payload) {
        if (failuresLeft.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            throw new IllegalStateException("Kafka is unavailable (simulated)");
        }
        sent.add(new Sent(topic, key, payload));
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }

    public void failNextSends(int count) {
        failuresLeft.set(count);
    }

    public void reset() {
        sent.clear();
        failuresLeft.set(0);
    }
}
