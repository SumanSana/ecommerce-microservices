package com.ecommerce.identityservice.service;

import com.ecommerce.identityservice.dto.RegisterRequestDTO;
import com.ecommerce.identityservice.dto.TokenRequestDTO;
import com.ecommerce.identityservice.dto.TokenResponseDTO;

public interface AuthService {
    void register(RegisterRequestDTO request);
    TokenResponseDTO generateToken(TokenRequestDTO request);
    TokenResponseDTO refreshToken(String refreshToken); // NEW
}