package com.ecommerce.paymentservice.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ekart/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiate(@RequestBody PaymentRequest request) throws Exception {
        log.info("Initiating payment for Order: {}", request.orderId());
        String clientSecret = paymentService.initiatePayment(request);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request, 
            @RequestHeader("Stripe-Signature") String sigHeader) {

        String payload;
        try {
            // 1. Read the RAW, untouched bytes from the request to avoid Signature failure
            payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read raw webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payload error");
        }

        Event event;
        try {
            // 2. Verify signature using raw payload with a 300-second timestamp tolerance
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret, 300L);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Webhook Signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 3. Deserialization Logic (Fix for dataObject returning null)
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElseGet(() -> {
            log.warn("API Version mismatch detected. Event API version: {}. Pinned Java Library version: {}. Attempting deserializeUnsafe()...", 
                     event.getApiVersion(), com.stripe.Stripe.API_VERSION);
            try {
                // Official Stripe fallback for API version mismatches in v28+
                return event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception ex) {
                log.error("deserializeUnsafe() failed completely: {}", ex.getMessage());
                return null;
            }
        });

        if (stripeObject == null) {
            log.error("Failed to extract data object from event {}", event.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data object");
        }

        // 4. Process the Event
        if ("payment_intent.succeeded".equals(event.getType()) || 
            "payment_intent.payment_failed".equals(event.getType())) {
            
            if (stripeObject instanceof PaymentIntent) {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                log.info("Processing {} for Intent: {}", event.getType(), paymentIntent.getId());
                paymentService.updatePaymentStatus(paymentIntent);
            } else {
                log.error("Expected PaymentIntent, but got: {}", stripeObject.getClass().getName());
            }
        } else {
            log.info("Unhandled event type received: {}", event.getType());
        }

        return ResponseEntity.ok("Success");
    }
}