package com.shop.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// PART C (check 3), together with OrderService.placeOrder
@Component
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    // REVIEW [Major] - RestTemplate with no timeouts waits forever on a hung
    // payment gateway, define a bean with connect and read timeouts
    private final RestTemplate restTemplate;
    private final String url;
    private final String apiKey;

    public RazorpayGateway(
            RestTemplate paymentRestTemplate,
            @Value("${payments.razorpay.url}") String url,
            @Value("${payments.razorpay.api-key}") String apiKey) {
        this.restTemplate = paymentRestTemplate;
        this.url = url;
        this.apiKey = apiKey;
    }

    // @Override
    // public String charge(String idempotencyKey, long amountPaise) {
    // log.info("Charging {} paise with key {}", amountPaise, apiKey);
    // // REVIEW [Major] - Map with no types found, use proper types for the
    // response
    // Map<?, ?> response = restTemplate.postForObject(url,
    // Map.of("amount", amountPaise, "reference", idempotencyKey), Map.class);
    // return (String) response.get("id");
    // }

    @Override
    public String charge(String idempotencyKey, long amountPaise) {

        log.info("Initiating payment for amount: {} paise", amountPaise);

        PaymentRequest request = new PaymentRequest(amountPaise, idempotencyKey);
        // REVIEW [Major] - Map with no types found, use proper types for the
        // response
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                url,
                request,
                PaymentResponse.class);

        PaymentResponse body = response.getBody();

        if (body == null || body.id() == null || body.id().isBlank()) {
            throw new IllegalStateException(
                    "Payment gateway returned an invalid response");
        }

        return body.id();
    }
}
