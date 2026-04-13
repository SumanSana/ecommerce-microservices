package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public record ProductViewDTO(String skuId, String name, BigDecimal price) {
}
