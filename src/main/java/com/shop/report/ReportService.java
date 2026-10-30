package com.shop.report;

import org.springframework.stereotype.Service;

import com.shop.order.OrderRepository;
import com.shop.order.OrderStatus;

// PART E (check 5), together with ReportRepository
@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;

    public ReportService(OrderRepository orderRepository, ReportRepository reportRepository) {
        this.orderRepository = orderRepository;
        this.reportRepository = reportRepository;
    }

    /** Shown on the ops dashboard, refreshed every 30 seconds. The orders table has 40 million rows. */
    public long paidOrderCount() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();
    }

    public long ordersInCity(String city) {
        return reportRepository.countOrdersInCity(city);
    }
}
