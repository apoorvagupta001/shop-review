package com.shop.payment;

public record PaymentRequest(
        long amount,
        String reference) {
}