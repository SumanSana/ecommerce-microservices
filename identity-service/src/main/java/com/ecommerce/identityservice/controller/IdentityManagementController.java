package com.ecommerce.identityservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.identityservice.dto.PasswordUpdateRequest;
import com.ecommerce.identityservice.dto.UserProfileDTO;
import com.ecommerce.identityservice.entity.User;
import com.ecommerce.identityservice.repository.UserRepository;

@RestController
@RequestMapping("/ekart/v1/identity")
public class IdentityManagementController {

	@Autowired
	private UserRepository repository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@PutMapping("/profile")
	public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileDTO profileDTO) {
	    return repository.findByEmail(profileDTO.email())
	        .<ResponseEntity<?>>map(user -> {
	            user.setFirstName(profileDTO.firstName());
	            user.setLastName(profileDTO.lastName());
	            user.setMobile(profileDTO.mobile());
	            repository.save(user);
	            return ResponseEntity.ok(profileDTO); // Success returns the DTO
	        })
	        .orElse(ResponseEntity.status(404).body(Map.of("message", "User not found")));
	} 

	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@RequestBody PasswordUpdateRequest request) {
	    User user = repository.findByEmail(request.email())
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
	        return ResponseEntity.badRequest().body(Map.of("message", "Current password does not match"));
	    }

	    user.setPassword(passwordEncoder.encode(request.newPassword()));
	    repository.save(user);

	    return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));
	}

}
