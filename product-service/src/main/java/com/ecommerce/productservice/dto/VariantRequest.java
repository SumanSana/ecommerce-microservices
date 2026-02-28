package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VariantRequest(
	    @NotBlank String skuId,
	    @NotNull BigDecimal price,
	    Map<String, Object> attributes
	) {}
