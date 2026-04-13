package com.ecommerce.identityservice.dto;

public record UserProfileDTO(
	    String firstName,
	    String lastName,
	    String email,
	    String mobile
	) {}