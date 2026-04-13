package com.ecommerce.orderservice.dto;

import java.util.UUID;

public record InventoryResponse(UUID orderId, Boolean status,
		String message) {
}