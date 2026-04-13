package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VariantUpdateDTO(
	    String skuId, // Used as the unique key for syncing
	    BigDecimal price,
	    String status,
	    Map<String, Object> attributes,
	    String imageUrl
	) {}