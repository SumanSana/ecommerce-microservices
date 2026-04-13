package com.ecommerce.paymentservice.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.paymentservice.dto.RefundEvent;
import com.ecommerce.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

	private final PaymentService paymentService;

	@KafkaListener(topics = "payment-refund-topic", containerFactory = "refund")
	public void handleOrderEvent(RefundEvent event) {

		try {
			paymentService.processRefund(event);
		} catch (Exception e) {
			log.error("Critical: Failed to route Payment Event for Order {}: {}", event.orderId().toString(),
					e.getMessage());
			// In production, you would send this to a Dead Letter Queue (DLQ)

		}
	}
}