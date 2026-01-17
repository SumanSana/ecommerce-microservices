package com.ecommerce.identityservice.service;

import com.ecommerce.identityservice.dto.RegisterRequestDTO;
import com.ecommerce.identityservice.dto.TokenRequestDTO;
import com.ecommerce.identityservice.dto.TokenResponseDTO;

public interface AuthService {

	public void register(RegisterRequestDTO request);
	public TokenResponseDTO generateToken(TokenRequestDTO request);
}
