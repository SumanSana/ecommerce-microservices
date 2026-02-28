package com.ecommerce.orderservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.orderservice.dto.Address;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ekart/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

	private final OrderService orderService;
	private final OrderRepository orderRepository;

	/**
	 * Entry point for placing an order. Returns 202 ACCEPTED
	 */
	@PostMapping
	public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
		log.info("Received order request for customer: {}", request.customerId());
		OrderResponse response = orderService.createOrder(request);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	/**
	 * Get the status of a specific order.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
		return orderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Get order history for a specific customer.
	 */
	@GetMapping("/customer/{customerId}")
	public ResponseEntity<List<Order>> getCustomerOrders(@PathVariable UUID customerId) {
		return ResponseEntity.ok(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
	}

	/**
	 * Cancel an order. Triggers a rollback in Inventory and potentially Payment.
	 */
	@PostMapping("/{id}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
		log.info("Request to cancel order: {}", id);
		orderService.cancelOrder(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Update shipping address. Note: Typically only allowed if order is still in
	 * 'PENDING' or 'CONFIRMED' but not yet 'SHIPPED'.
	 */
	@PatchMapping("/{id}/address")
	public ResponseEntity<Void> updateShippingAddress(@PathVariable UUID id, @RequestBody Address newAddress) {
		orderService.updateShippingAddress(id, newAddress);
		return ResponseEntity.ok().build();
	}
}