package com.ecommerce.orderservice.processor;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Scheduled(fixedDelay = 5000)
	@Transactional
	public void relayEvents() {
		List<OutboxEvent> events = outboxRepository.findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();

		for (OutboxEvent event : events) {
			try {

				String topic = determineTopic(event.getAggregateType());
				kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).whenComplete((result, ex) -> {
					if (ex == null) {
						log.info("Successfully relayed event: {}", event.getId());
					} else {
						log.error("Failed to relay event: {}", event.getId());
					}
				});

				event.setProcessedAt(Instant.now());
				outboxRepository.save(event);
			} catch (Exception e) {
				log.error("Error processing outbox event {}: {}", event.getId(), e.getMessage());
			}
		}
	}

	private String determineTopic(String aggregateType) {
		return switch (aggregateType) {
		case "ORDER" -> "order-events-topic";
		case "PAYMENT" -> "payment-refund-topic";
		default -> "general-outbox-topic";
		};
	}
}