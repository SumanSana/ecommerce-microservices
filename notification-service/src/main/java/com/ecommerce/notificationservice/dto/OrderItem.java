package com.ecommerce.notificationservice.dto;

import java.math.BigDecimal;

public record OrderItem(String skuId, BigDecimal price, int quantity) {
}