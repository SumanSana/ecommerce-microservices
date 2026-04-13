package com.ecommerce.orderservice.dto;

import java.util.UUID;

public record PaymentResponse(UUID orderId, String paymentStatus) {
}