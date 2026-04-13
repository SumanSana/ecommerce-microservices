package com.ecommerce.inventoryservice.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	// 1. Handle Bean Validation Errors (e.g., @NotBlank, @Min, @Email) -> 400 Bad
	// Request
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
		String errorMessage = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));

		log.warn("Validation failed: {}", errorMessage);
		return buildResponse(HttpStatus.BAD_REQUEST, errorMessage);
	}

	// 2. Handle Manual Validation from Record Compact Constructors -> 400 Bad
	// Request
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Illegal argument: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	// 3. Handle "SKU Not Found" -> 404 Not Found
	@ExceptionHandler(SkuNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(SkuNotFoundException ex) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	// 4. Handle "Insufficient Stock" -> 409 Conflict
	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	// 5. Handle Database Optimistic Locking -> 409 Conflict
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ObjectOptimisticLockingFailureException ex) {
		return buildResponse(HttpStatus.CONFLICT, "The resource was updated by another process. Please try again.");
	}

	// 6. Fallback for all other unexpected errors -> 500 Internal Server Error
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralError(Exception ex) {
		log.error("Unhandled exception occurred: ", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
		ErrorResponse error = new ErrorResponse(status.value(), message, LocalDateTime.now());
		return new ResponseEntity<>(error, status);
	}

	// Response DTO
	public record ErrorResponse(int status, String message, LocalDateTime timestamp) {
	}
}