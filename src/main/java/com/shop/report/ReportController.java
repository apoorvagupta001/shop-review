package com.shop.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports/orders-by-city")
    public long ordersByCity(@RequestParam("city") String city) {
        return reportService.ordersInCity(city);
    }

    @GetMapping("/reports/paid-orders")
    public long paidOrders() {
        return reportService.paidOrderCount();
    }
}
