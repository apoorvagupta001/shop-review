package com.shop;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.shop.payment.PaymentGateway;

/** Test stand-in for the payment gateway. Records how often it was called, and whether a transaction was open. */
public class RecordingPaymentGateway implements PaymentGateway {

    private final AtomicInteger charges = new AtomicInteger();
    private volatile boolean chargedInsideTransaction;

    @Override
    public String charge(String idempotencyKey, long amountPaise) {
        charges.incrementAndGet();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            chargedInsideTransaction = true;
        }
        return "pay_" + UUID.randomUUID();
    }

    public int charges() {
        return charges.get();
    }

    public boolean chargedInsideTransaction() {
        return chargedInsideTransaction;
    }

    public void reset() {
        charges.set(0);
        chargedInsideTransaction = false;
    }
}
