package com.ecommerce.productservice.entity;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantView {
	private String skuId;
	private BigDecimal price; // Price stays here!
	private Integer availableQuantity;
	private String status;
	private boolean inStock;
	private Map<String, Object> attributes;
	private String imageUrl;
}