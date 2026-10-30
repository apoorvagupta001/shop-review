package com.shop.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(@NotBlank String customerId, @NotBlank String sku,
        @Min(1) @Max(10) int qty, @NotBlank String city, String couponCode) {
}
