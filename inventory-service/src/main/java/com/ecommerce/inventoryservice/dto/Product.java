package com.ecommerce.inventoryservice.dto;

import java.time.Instant;
import java.util.List;

public record Product(String productId, String name, String description, String brandName, String categoryName,
		String status, List<ProductVariant> variants, Instant timestamp) {
}