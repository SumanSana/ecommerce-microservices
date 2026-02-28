package com.ecommerce.productservice.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(@NotBlank String name, String description, @NotNull UUID brandId,
		@NotNull UUID categoryId, @NotEmpty List<VariantRequest> variants) {
}