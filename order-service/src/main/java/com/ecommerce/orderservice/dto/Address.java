package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Address(@NotBlank(message = "Address line 1 is required") @Size(max = 255) String shippingAddressLine1,

		@NotBlank(message = "City is required") @Size(max = 100) String shippingCity,

		@NotBlank(message = "Zip code is required") @Size(max = 20) String shippingZipCode,

		@NotBlank(message = "Country is required") @Size(max = 100) String shippingCountry) {
}