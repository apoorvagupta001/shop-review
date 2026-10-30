package com.shop.payment;

public interface PaymentGateway {

    /**
     * Charges the customer and returns the payment id.
     * A real network call: it is slow, and it can fail or time out.
     */
    String charge(String idempotencyKey, long amountPaise);
}
