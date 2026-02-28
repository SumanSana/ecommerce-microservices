package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentEvent(UUID orderId, String paymentId, BigDecimal amount, String status, String customerEmail) {

}