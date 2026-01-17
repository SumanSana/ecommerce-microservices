package com.ecommerce.identityservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank
        @Size(max = 100)
        String userName,

        @Email
        @NotBlank
        @Size(max = 150)
        String email,

        @NotBlank
        @Pattern(regexp="\\d{10}")
        String mobileNumber,

        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {}
