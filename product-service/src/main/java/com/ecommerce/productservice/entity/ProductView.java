package com.ecommerce.productservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Document(collection = "product_views")
@Data
@Builder
public class ProductView {

	@Id
	private String id;

	private String name;
	private String description;

	private String brandName;
	private String categoryName;
	private String status;

	@Indexed(unique = true) // Speeds up lookup by SKU
	private String skuId;
	private BigDecimal price;

	// All dynamic attributes at the top level for easy Mongo querying
	private Map<String, Object> attributes;

	@CreatedDate
	private Instant firstSyncAt;

	@LastModifiedDate
	private Instant lastSyncedAt;
}