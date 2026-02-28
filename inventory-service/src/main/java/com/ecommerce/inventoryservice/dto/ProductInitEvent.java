package com.ecommerce.inventoryservice.dto;

import java.util.Map;

public record ProductInitEvent(
	    String id,
	    String name,
	    String skuId,
	    String brandName,
	    Map<String, Object> attributes,
	    String categoryName
	) {}