package com.ecommerce.apigateway.util;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	private final SecretKey key;

	JwtUtil(@Value("${jwt.secret") String secret) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}

	public Claims validateAndGetClaims(String token) {

		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
