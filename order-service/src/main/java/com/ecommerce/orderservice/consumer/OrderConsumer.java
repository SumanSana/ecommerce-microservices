package com.ecommerce.orderservice.consumer;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.orderservice.dto.InventoryResponse;
import com.ecommerce.orderservice.dto.PaymentEvent;
import com.ecommerce.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

	private final OrderService orderService;

	/**
	 * Listens for Inventory Service results.
	 */
	@KafkaListener(topics = "inventory-response-topic", containerFactory = "inventoryResponse")
	public void consumeInventoryResponse(InventoryResponse event) {
		UUID orderId = event.orderId();
		boolean success = event.status();

		log.info("Received inventory response for order {}: success={}", orderId, success);
		orderService.handleInventoryResponse(orderId, success);
	}

	/**
	 * Listens for Payment Service results (Stripe Webhook results).
	 */
	@KafkaListener(topics = "payment-response-topic", containerFactory = "paymentEvent")
	public void consumePaymentResponse(PaymentEvent event) {
		UUID orderId = event.orderId();
		String paymentStatus = event.status();
		log.info("Received payment response for order {}: paymentStatus={}", orderId, paymentStatus);
		orderService.handlePaymentResponse(orderId, paymentStatus);
	}
}