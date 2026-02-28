package com.ecommerce.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull(message = "Order ID is required") UUID orderId,

		@NotBlank(message = "Customer email is required") @Email(message = "Invalid email format") String customerEmail,

		@NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount) {
}