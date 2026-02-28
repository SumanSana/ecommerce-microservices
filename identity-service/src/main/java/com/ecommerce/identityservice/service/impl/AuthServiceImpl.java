package com.ecommerce.identityservice.service.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.identityservice.constant.Constants;
import com.ecommerce.identityservice.dto.RegisterRequestDTO;
import com.ecommerce.identityservice.dto.TokenRequestDTO;
import com.ecommerce.identityservice.dto.TokenResponseDTO;
import com.ecommerce.identityservice.entity.Role;
import com.ecommerce.identityservice.entity.User;
import com.ecommerce.identityservice.entity.UserStatus;
import com.ecommerce.identityservice.repository.RoleRepository;
import com.ecommerce.identityservice.repository.UserRepository;
import com.ecommerce.identityservice.service.AuthService;
import com.ecommerce.identityservice.service.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Override
	public void register(RegisterRequestDTO request) {

		if (userRepository.existsByMobileNumber(request.mobileNumber())) {
			throw new IllegalArgumentException("Mobile number already registered");
		}

		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Email already registered");
		}

		Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

		User user = User.builder().userName(request.userName()).mobileNumber(request.mobileNumber()).email(request.email())
				.password(passwordEncoder.encode(request.password())).roles(Set.of(userRole)).status(UserStatus.ACTIVE)
				.build();

		userRepository.save(user);
	}

	@Override
	public TokenResponseDTO generateToken(TokenRequestDTO request) {

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid credentials");
		}

		if (!user.getStatus().equals(UserStatus.ACTIVE))
			throw new IllegalArgumentException("User is not active");

		String token = jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRoles());

		return new TokenResponseDTO(token, "Bearer", Constants.EXPIRATION_MS + "ms");
	}

}
