package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID orderId, String customerEmail, BigDecimal amount) {
}