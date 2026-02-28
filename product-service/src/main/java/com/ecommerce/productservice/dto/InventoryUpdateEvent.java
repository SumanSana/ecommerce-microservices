package com.ecommerce.productservice.dto;

public record InventoryUpdateEvent(String skuId, int availableQuantity, String status, String updatedAt) {
}