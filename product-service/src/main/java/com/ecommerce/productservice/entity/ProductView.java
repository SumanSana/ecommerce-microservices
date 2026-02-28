package com.ecommerce.productservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "product_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductView {

	@Id
	private String skuId;
	private String productId;
	private String name;
	private String description;
	private String brandName;
	private String categoryName;
	private String status;
	private BigDecimal price;

	private Integer availableQuantity;
	private boolean inStock;
	private String stockLabel;

	private Map<String, Object> attributes;

	@Version
	private Long version;

	@CreatedDate
	private Instant firstSyncAt;

	@LastModifiedDate
	private Instant lastSyncedAt;
}