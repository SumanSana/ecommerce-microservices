package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductSyncEvent(
	    String productId,
	    String name,
	    String skuId,
	    String status,
	    String brandName,
	    String description,
	    BigDecimal price,
	    String categoryName,
	    Map<String, Object> attributes,
	    Long timestamp
	) {}