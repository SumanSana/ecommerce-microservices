package com.ecommerce.inventoryservice.dto;

import java.util.UUID;

public record InventoryResponseEvent(UUID orderId, Boolean status,
		String message) {
}