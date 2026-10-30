package com.shop.events;

import java.util.UUID;

public record OrderCancelledEvent(UUID eventId, String type, long orderId, String customerId, long totalPaise) {
}