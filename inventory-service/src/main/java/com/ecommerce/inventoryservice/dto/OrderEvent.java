package com.ecommerce.inventoryservice.dto;

import java.util.List;
import java.util.UUID;

public record OrderEvent(UUID orderId, String customerId, String customerEmail, List<OrderItem> items, String status) {
}