package com.ecommerce.orderservice.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException; // MUST BE THIS ONE
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing 
                ));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    // 2. Handle Feign Client Errors (e.g., Product Service returns 404)
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(FeignException e) {
        return buildResponse(HttpStatus.NOT_FOUND, "One or more products in your cart no longer exist.", null);
    }

    // 3. Handle Feign General Errors (e.g., Payment Service is down)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Communication with downstream service failed.", null);
    }

    // 4. Fallback for all other errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, Map<String, String> errors) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                message,
                errors,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, status);
    }

    // Standardized JSON Response Structure
    public record ErrorResponse(
            int status,
            String message,
            Map<String, String> errors, // This will be null for non-validation errors
            LocalDateTime timestamp
    ) {}
}