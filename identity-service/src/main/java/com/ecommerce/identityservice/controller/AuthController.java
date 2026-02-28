package com.ecommerce.identityservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.identityservice.dto.TokenRequestDTO;
import com.ecommerce.identityservice.dto.TokenResponseDTO;
import com.ecommerce.identityservice.dto.RegisterRequestDTO;
import com.ecommerce.identityservice.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/ekart/v1/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
		authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/token")
	public ResponseEntity<TokenResponseDTO> generateToken(@Valid @RequestBody TokenRequestDTO request) {
		return ResponseEntity.ok(authService.generateToken(request));
	}
}
