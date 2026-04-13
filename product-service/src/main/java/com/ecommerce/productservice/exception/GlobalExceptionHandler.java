package com.ecommerce.productservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// CORRECT IMPORT for REST Validation
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Catches @Valid failures in your Controller. Returns 400 Bad Request with a
	 * map of field names and error messages.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	/**
	 * Catches Business Logic errors (e.g., "Brand not found", "Insufficient
	 * Stock"). Returns 404 Not Found.
	 */
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, String>> handleRuntimeErrors(RuntimeException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		error.put("status", "NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * The "Safety Net": Catches any other unexpected errors (e.g., DB connection
	 * loss). Prevents raw stack traces from leaking to the UI.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGeneralErrors(Exception ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", "An unexpected error occurred. Please try again later.");
		error.put("details", ex.getMessage()); // Remove 'details' in production for security
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}