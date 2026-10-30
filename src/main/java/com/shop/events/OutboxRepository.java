package com.shop.events;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    // Order by id, not createdAt: two rows can share a timestamp, but ids always
    // increase
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
