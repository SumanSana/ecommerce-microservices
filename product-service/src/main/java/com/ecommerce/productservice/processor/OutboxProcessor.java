package com.ecommerce.productservice.processor;

import java.time.Instant;
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

	@Scheduled(fixedDelay = 5000)
	@Transactional
	public void relayEvents() {

		List<OutboxEvent> events = outboxRepository.findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();

		for (OutboxEvent event : events) {
			try {

				log.info("DEBUG: Sending Payload -> {}", event.getPayload());
				kafkaTemplate.send("product-sync-topic", event.getAggregateId(), event.getPayload());
				if (event.getType().contains("PRODUCT"))
					kafkaTemplate.send("inventory-init-topic", event.getAggregateId(), event.getPayload());
				
				event.setProcessedAt(Instant.now());
				outboxRepository.save(event);
				log.info("Relayed event {} to Kafka topic", event.getId());
			} catch (Exception e) {
				log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
			}
		}
	}
}