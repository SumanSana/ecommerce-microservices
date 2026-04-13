package com.ecommerce.productservice.entity;

import java.time.Instant;
import java.util.List;

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
	private String productId;
	private String name;
	private String description;
	private String brandName;
	private String categoryName;
	private String status;

	// The Source of Truth for prices
	private List<VariantView> variants;

	@Version
	private Long version;

	@CreatedDate
	private Instant firstSyncAt;

	@LastModifiedDate
	private Instant lastSyncedAt;

}