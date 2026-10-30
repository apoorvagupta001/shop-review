package com.shop.order;

public record OrderResponse(Long id, OrderStatus status, long totalPaise) {
    static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalPaise());
    }
}
