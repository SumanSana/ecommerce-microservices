package com.ecommerce.identityservice.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDTO(

        @NotBlank
        String mobileNumber,

        @NotBlank
        String password
) {}
