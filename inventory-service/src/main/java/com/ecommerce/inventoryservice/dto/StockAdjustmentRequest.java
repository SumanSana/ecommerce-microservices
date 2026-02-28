package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StockAdjustmentRequest(@NotBlank(message = "SKU ID is required") String skuId,

		@NotBlank(message = "Transaction type is required") String transactionType,

		@Min(value = 1, message = "Adjustment must be at least 1") int adjustment,

		@NotBlank(message = "Reason is required") String reason) {
}