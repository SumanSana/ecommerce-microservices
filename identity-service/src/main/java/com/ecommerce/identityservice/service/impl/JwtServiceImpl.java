package com.ecommerce.identityservice.service.impl;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ecommerce.identityservice.constant.Constants;
import com.ecommerce.identityservice.entity.Role;
import com.ecommerce.identityservice.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

	private SecretKey key;

	JwtServiceImpl(@Value("${jwt.secret}") String secret) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}

	@Override
	public String generateToken(String userId, String mobileNumber, Set<Role> roles) {

		Instant now = Instant.now();

		return Jwts.builder().subject(userId).claim("mobile", mobileNumber).claim("roles", roles)
				.issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(Constants.EXPIRATION_MS)))
				.signWith(key, Jwts.SIG.HS256).compact();
	}

	@Override
	public Claims validateAndGetClaims(String token) {

		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
