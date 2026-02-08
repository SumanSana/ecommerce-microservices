package com.ecommerce.productservice.processor;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.repository.OutboxRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	// Runs every 5 seconds
	@Scheduled(fixedDelay = 5000)
	@Transactional
	public void relayEvents() {

		// Fetch unprocessed events
		List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

		for (OutboxEvent event : events) {
			try {

				log.info("DEBUG: Sending Payload -> {}", event.getPayload());
				// Publish to Kafka, We use the aggregateId as the Kafka Key to ensure ordering
				kafkaTemplate.send("product-sync-topic", event.getAggregateId(), event.getPayload());

				// Mark as processed
				event.setProcessed(true);
				outboxRepository.save(event);

				log.info("Relayed event {} to Kafka topic", event.getId());
			} catch (Exception e) {
				log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
				// We don't mark as processed, so it will retry in the next 5 seconds
			}
		}
	}
}