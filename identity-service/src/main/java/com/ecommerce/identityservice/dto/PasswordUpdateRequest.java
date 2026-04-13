package com.ecommerce.identityservice.dto;

public record PasswordUpdateRequest(
	    String oldPassword,
	    String newPassword,
	    String email 
	) {}