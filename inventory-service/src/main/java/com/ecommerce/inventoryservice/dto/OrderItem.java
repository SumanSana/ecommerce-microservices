package com.ecommerce.inventoryservice.dto;

import java.math.BigDecimal;

public record OrderItem(String skuId, int quantity, BigDecimal price) {
}