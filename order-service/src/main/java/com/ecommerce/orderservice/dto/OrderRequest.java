package com.ecommerce.orderservice.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotNull UUID customerId, @NotBlank String customerEmail, @NotBlank String addressLine1,
		@NotBlank String city, @NotBlank String zipCode, @NotBlank String country,
		@NotEmpty List<OrderItemRequest> items) {
}