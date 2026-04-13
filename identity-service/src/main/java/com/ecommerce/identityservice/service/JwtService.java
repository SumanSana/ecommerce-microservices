package com.ecommerce.identityservice.service;

import com.ecommerce.identityservice.entity.User;

import io.jsonwebtoken.Claims;

public interface JwtService {

	public Claims validateAndGetClaims(String token);

	public String generateRefreshToken(String userId);

	String generateToken(User user);
}
