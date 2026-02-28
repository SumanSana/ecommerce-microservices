package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(@NotBlank String skuId, @NotBlank Integer quantity) {
}