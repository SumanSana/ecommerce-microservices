package com.ecommerce.paymentservice.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stripe.exception.StripeException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(StripeException.class)
	public ResponseEntity<?> handleStripeException(StripeException e) {
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
				.body(Map.of("error", "Stripe Gateway Error", "message", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Invalid Request", "message", e.getMessage()));
	}
}