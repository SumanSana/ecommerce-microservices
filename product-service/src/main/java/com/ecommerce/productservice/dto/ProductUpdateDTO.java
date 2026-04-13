package com.ecommerce.productservice.dto;

import java.util.List;
import java.util.UUID;

public record ProductUpdateDTO(UUID brandId, UUID categoryId, String name, String status, String description, List<VariantUpdateDTO> variants) {
}