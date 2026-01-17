package com.ecommerce.identityservice.service;

import java.util.Set;

import com.ecommerce.identityservice.entity.Role;

import io.jsonwebtoken.Claims;

public interface JwtService {

	public Claims validateAndGetClaims(String token);

	public String generateToken(String userId, String mobileNumber, Set<Role> roles);
}
