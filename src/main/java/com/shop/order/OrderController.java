package com.shop.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

// PART F (checks 6 and 7)

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Places an order from the app. The client sends customerId, sku, qty, city and
     * an optional couponCode.
     * Responds 201 Created with the new order, or 400 if the request is invalid.
     */

    // REVIEW - [Blocker] - No validations found in Request body, this can result in
    // garbage values being set to the Object, Add validations and not null checks
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody PlaceOrderRequest req)
            throws CouponExpiredException {
        Order order = orderService.placeOrder(req.customerId(), req.sku(), req.qty(), req.city(), req.couponCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable("id") Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}
