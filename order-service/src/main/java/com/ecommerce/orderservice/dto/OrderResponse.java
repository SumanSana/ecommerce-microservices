package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID orderId, String status, BigDecimal totalAmount, String paymentIntentId) {
}