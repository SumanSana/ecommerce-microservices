package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

//IMPORTANT: Use jakarta, not javax
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductCreateRequest(
 @NotBlank(message = "Product name cannot be blank")
 String name,
 
 String description,
 
 @NotNull(message = "Brand ID is required")
 UUID brandId,
 
 @NotNull(message = "Category ID is required")
 UUID categoryId,
 
 @NotBlank(message = "SKU is required")
 String skuId,
 
 @Positive(message = "Price must be greater than zero")
 BigDecimal price,
 
 Map<String, Object> attributes
) {}