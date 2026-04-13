package com.ecommerce.inventoryservice.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductVariant(@NotBlank String skuId, @NotNull BigDecimal price, Map<String, Object> attributes) {
}
