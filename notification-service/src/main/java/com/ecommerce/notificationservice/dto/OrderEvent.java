package com.ecommerce.notificationservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderEvent(UUID orderId, String customerEmail, List<OrderItem> items, BigDecimal amount, String status) {
}