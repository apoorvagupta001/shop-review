package com.shop.order;

public record OrderSummary(Long orderId, int itemCount, long totalPaise) {
}
