package com.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.shop.catalog.Product;
import com.shop.catalog.ProductRepository;
import com.shop.inventory.InventoryService;
import com.shop.inventory.OutOfStockException;
import com.shop.order.CouponExpiredException;
import com.shop.order.Order;
import com.shop.order.OrderHistoryService;
import com.shop.order.OrderRepository;
import com.shop.order.OrderService;
import com.shop.order.OrderStatus;
import com.shop.order.OrderSummary;
import com.shop.report.ReportRepository;

import jakarta.persistence.EntityManagerFactory;

/*
 * DAY 4 CHECKS: do not change this file.
 * Run it with `mvn test`, or with the Run/Test button in your IDE.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(Day4ChecksTest.TestPayments.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class Day4ChecksTest {

    @TestConfiguration
    static class TestPayments {
        @Bean
        @Primary
        RecordingPaymentGateway recordingPaymentGateway() {
            return new RecordingPaymentGateway();
        }
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private RecordingPaymentGateway payments;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private OrderService orderService;
    @Autowired private OrderHistoryService orderHistoryService;
    @Autowired private ReportRepository reportRepository;

    @BeforeEach
    void resetData() {
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        jdbc.update("delete from products");
        productRepository.save(new Product("MOB-1", "Phone", 49_900, 5));
        productRepository.save(new Product("LAP-1", "Laptop", 99_900, 5));
        productRepository.save(new Product("TSHIRT-1", "T-shirt", 59_900, 0));
        productRepository.save(new Product("PEN-1", "Pen", 1_000, 1_000));
        payments.reset();
    }

    @Test
    @DisplayName("1. Cart reservation is all or nothing")
    void check1_cartReservationIsAllOrNothing() {
        assertThrows(OutOfStockException.class,
                () -> inventoryService.reserveCart(List.of("MOB-1", "LAP-1", "TSHIRT-1")),
                "TSHIRT-1 has no stock, so reserveCart must throw OutOfStockException");
        assertEquals(5, stockOf("MOB-1"), "MOB-1 stayed reserved although the cart failed (stock should still be 5)");
        assertEquals(5, stockOf("LAP-1"), "LAP-1 stayed reserved although the cart failed (stock should still be 5)");
    }

    @Test
    @DisplayName("2. An expired coupon leaves no order, no stock change and no charge")
    void check2_expiredCouponLeavesNothingBehind() {
        assertThrows(CouponExpiredException.class,
                () -> orderService.placeOrder("C-1", "MOB-1", 1, "Bengaluru", "EXPIRED50"),
                "an expired coupon must be rejected with CouponExpiredException");
        assertEquals(0, orderRepository.count(), "an order was saved although the coupon was rejected");
        assertEquals(5, stockOf("MOB-1"), "stock was taken although the order failed (should still be 5)");
        assertEquals(0, payments.charges(), "the customer was charged although the order failed");
    }

    @Test
    @DisplayName("3. The payment gateway is never called inside a database transaction")
    void check3_paymentOutsideTransaction() throws Exception {
        orderService.placeOrder("C-1", "MOB-1", 1, "Bengaluru", null);
        assertEquals(1, payments.charges(), "the customer should be charged exactly once");
        assertFalse(payments.chargedInsideTransaction(),
                "charge() ran while a database transaction was open: a slow gateway holds the connection and row locks");
        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size(), "exactly one order should be saved");
        assertEquals(OrderStatus.PAID, orders.get(0).getStatus(), "the saved order should be PAID");
        assertNotNull(orders.get(0).getPaymentId(), "the payment id should be saved on the order");
    }

    @Test
    @DisplayName("4. Order history for 20 orders runs at most 2 SQL queries")
    void check4_noNPlusOne() throws Exception {
        for (int i = 0; i < 20; i++) {
            orderService.placeOrder("C-7", "PEN-1", 1, "Bengaluru", null);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<OrderSummary> summaries = orderHistoryService.summaries("C-7");

        long queries = statistics.getPrepareStatementCount();
        assertEquals(20, summaries.size(), "the customer has 20 orders");
        assertTrue(summaries.stream().allMatch(s -> s.itemCount() == 1), "every order has exactly 1 item");
        assertTrue(queries <= 2, "order history ran " + queries + " SQL queries for 20 orders (expected at most 2): N+1");
    }

    @Test
    @DisplayName("5. The city report cannot be tricked by the search text")
    void check5_cityReportIsSafe() throws Exception {
        orderService.placeOrder("C-1", "PEN-1", 1, "Bengaluru", null);
        orderService.placeOrder("C-2", "PEN-1", 1, "Bengaluru", null);
        orderService.placeOrder("C-3", "PEN-1", 1, "Mumbai", null);

        assertEquals(2, reportRepository.countOrdersInCity("Bengaluru"), "Bengaluru has 2 orders");
        assertEquals(0, reportRepository.countOrdersInCity("x' or '1'='1"),
                "searching for x' or '1'='1 returned orders: SQL injection");
        assertEquals(0, reportRepository.countOrdersInCity("D'Souza Nagar"), "a city name with a quote must work");
    }

    @Test
    @DisplayName("6. A client cannot set the order status or total")
    void check6_clientCannotSetStatusOrTotal() throws Exception {
        MvcResult result = mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "C-9", "sku": "MOB-1", "qty": 2, "city": "Bengaluru",
                                 "status": "DELIVERED", "totalPaise": 1}
                                """))
                .andReturn();

        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size(), "exactly one order should be saved");
        assertNotEquals(OrderStatus.DELIVERED, orders.get(0).getStatus(),
                "the client set status = DELIVERED on a new order");
        assertEquals(99_800, orders.get(0).getTotalPaise(),
                "the client set the total: 2 phones at Rs 499 must cost 99800 paise");
        assertEquals(201, result.getResponse().getStatus(), "a created order should return 201 Created");
    }

    @Test
    @DisplayName("7. An invalid quantity is rejected with 400 Bad Request")
    void check7_invalidQuantityIsRejected() throws Exception {
        MvcResult result = mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "C-9", "sku": "MOB-1", "qty": -3, "city": "Bengaluru"}
                                """))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus(), "qty = -3 must be rejected with 400 Bad Request");
        assertEquals(0, orderRepository.count(), "an order was saved for qty = -3");
    }

    private int stockOf(String sku) {
        return productRepository.findBySku(sku).orElseThrow().getStock();
    }
}
