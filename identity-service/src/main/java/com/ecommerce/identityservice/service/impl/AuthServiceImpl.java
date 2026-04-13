package com.ecommerce.identityservice.service.impl;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.identityservice.constant.Constants;
import com.ecommerce.identityservice.dto.RegisterRequestDTO;
import com.ecommerce.identityservice.dto.TokenRequestDTO;
import com.ecommerce.identityservice.dto.TokenResponseDTO;
import com.ecommerce.identityservice.entity.User;
import com.ecommerce.identityservice.entity.UserRoles;
import com.ecommerce.identityservice.entity.UserStatus;
import com.ecommerce.identityservice.repository.UserRepository;
import com.ecommerce.identityservice.service.AuthService;
import com.ecommerce.identityservice.service.JwtService;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private JwtService jwtService;

	@Override
	public void register(RegisterRequestDTO request) {
		if (userRepository.existsByMobile(request.mobile())) {
			throw new IllegalArgumentException("Mobile number already registered");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Email already registered");
		}


		User user = User.builder().firstName(request.firstName()).lastName(request.lastName()).mobile(request.mobile())
				.email(request.email()).password(passwordEncoder.encode(request.password())).role(UserRoles.USER)
				.status(UserStatus.ACTIVE).build();

		userRepository.save(user);
	}

	@Override
	public TokenResponseDTO generateToken(TokenRequestDTO request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid credentials");
		}

		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new IllegalArgumentException("User is not active");
		}

		return createTokenResponse(user);
	}

	// NEW: Refresh Token Logic
	@Override
	public TokenResponseDTO refreshToken(String refreshToken) {
		// Validate the incoming refresh token and extract the User ID
		Claims claims = jwtService.validateAndGetClaims(refreshToken);
		String userId = claims.getSubject();

		// Verify the user still exists and is active in the database
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new IllegalArgumentException("User is not active");
		}

		// Generate a fresh pair of tokens
		return createTokenResponse(user);
	}

	// NEW: Helper method to keep token generation DRY
	private TokenResponseDTO createTokenResponse(User user) {
        
		String accessToken = jwtService.generateToken(user);

        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

        return new TokenResponseDTO(
        		user.getId().toString(), 
                user.getFirstName(), 
                user.getLastName(), 
                user.getMobile(),
                user.getEmail(), 
                user.getRole().toString(),
                accessToken,
                refreshToken,
                "Bearer",
                Constants.EXPIRATION_MS / 1000, 
                Constants.REFRESH_EXPIRATION_MS /1000
                
        );
    }
}