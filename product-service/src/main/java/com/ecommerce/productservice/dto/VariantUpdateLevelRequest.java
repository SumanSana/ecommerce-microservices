package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VariantUpdateLevelRequest(
	    BigDecimal price,
	    Map<String, Object> attributes
	) {}