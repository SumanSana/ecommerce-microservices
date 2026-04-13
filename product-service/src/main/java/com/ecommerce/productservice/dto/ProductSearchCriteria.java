package com.ecommerce.productservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchCriteria {
	private String brandName;
	private String categoryName;
	private String searchTerm;
	private Boolean inStockOnly;

	@Min(value = 0, message = "Price cannot be negative")
	private BigDecimal minPrice;

	@Min(value = 0, message = "Price cannot be negative")
	private BigDecimal maxPrice;

	public boolean isValidPriceRange() {
		if (minPrice != null && maxPrice != null) {
			return minPrice.compareTo(maxPrice) <= 0;
		}
		return true;
	}
}