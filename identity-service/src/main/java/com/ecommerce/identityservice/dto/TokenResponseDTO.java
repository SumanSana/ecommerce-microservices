package com.ecommerce.identityservice.dto;

public record TokenResponseDTO(
        String accessToken,
        String tokenType,
        String expiry
) {}
