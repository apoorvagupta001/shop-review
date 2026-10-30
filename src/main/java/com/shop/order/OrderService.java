
package com.shop.order;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.shop.catalog.Product;
import com.shop.catalog.ProductRepository;
import com.shop.inventory.OutOfStockException;
import com.shop.payment.PaymentGateway;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CouponService couponService;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CouponService couponService,
            PaymentGateway paymentGateway,
            PlatformTransactionManager transactionManager) {

        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.couponService = couponService;
        this.paymentGateway = paymentGateway;

        this.transactionTemplate = new TransactionTemplate(transactionManager);

        this.transactionTemplate.setTimeout(5);
    }

    /**
     * Places an order and charges the customer. couponCode may be null.
     * If anything fails, no order may be saved, no stock taken and nobody charged.
     */

    // REVIEW - [Major] - No timeout, rollback for transaction defined.
    public Order placeOrder(
            String customerId,
            String sku,
            int qty,
            String city,
            String couponCode) throws CouponExpiredException {

        // Transaction 1: Validate and save order
        Order order;

        try {
            order = transactionTemplate.execute(status -> {
                try {
                    Product product = productRepository.findBySku(sku)
                            .orElseThrow(() -> new IllegalArgumentException("Unknown SKU " + sku));

                    // REVIEW - [Blocker] - (product.getStock() - qty) this can cause race
                    // condition, leading to over selling or lost updates. Use conditional updates

                    if (qty <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive");
                    }

                    // Validate coupon before modifying stock
                    if (couponCode != null) {
                        couponService.validate(couponCode);
                    }

                    int updatedStock = productRepository.reduceStock(sku, qty);

                    if (updatedStock == 0) {
                        throw new OutOfStockException(sku, qty);
                    }

                    // Do not call product.setStock() here.
                    // reduceStock() has already updated the database.

                    Order newOrder = new Order();
                    newOrder.setCustomerId(customerId);
                    newOrder.setCity(city);
                    newOrder.setStatus(OrderStatus.PENDING_PAYMENT);
                    newOrder.setPlacedAt(Instant.now());
                    newOrder.addItem(new OrderItem(
                            sku, qty, product.getPricePaise()));

                    long totalPaise = product.getPricePaise() * qty;

                    if (couponCode != null) {
                        newOrder.setCouponCode(couponCode);
                        totalPaise = couponService.apply(totalPaise, couponCode);
                    }

                    newOrder.setTotalPaise(totalPaise);

                    return orderRepository.save(newOrder);

                } catch (CouponExpiredException e) {
                    status.setRollbackOnly();
                    throw new CouponValidationRuntimeException(e);
                }
            });
        } catch (CouponValidationRuntimeException e) {
            throw (CouponExpiredException) e.getCause();
        }

        // REVIEW [Blocker] - HTTP call within transaction, this will lead to threads in
        // waiting and eventually timedout, move charge call outside the transaction.

        // Payment call happens OUTSIDE the transaction
        String paymentId = paymentGateway.charge(
                "order-" + order.getId(),
                order.getTotalPaise());

        // Transaction 2: Update payment status
        return transactionTemplate.execute(status -> {
            Order savedOrder = orderRepository.findById(order.getId())
                    .orElseThrow();

            savedOrder.setPaymentId(paymentId);
            savedOrder.setStatus(OrderStatus.PAID);

            return savedOrder;
        });
    }

    // Used to carry the checked exception through TransactionTemplate's callback
    private static class CouponValidationRuntimeException extends RuntimeException {
        CouponValidationRuntimeException(CouponExpiredException cause) {
            super(cause);
        }
    }
}
