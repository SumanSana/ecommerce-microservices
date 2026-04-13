package com.ecommerce.identityservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponseDTO(String id, String firstName, String lastName, String mobile, String email, String role,
		@JsonProperty("access_token") String accessToken,

		@JsonProperty("refresh_token") String refreshToken,

		@JsonProperty("token_type") String tokenType,

		@JsonProperty("expires_in") Long expiresIn,

		@JsonProperty("refresh_expires_in") Long refreshExpiresIn) {
}