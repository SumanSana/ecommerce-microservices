package com.ecommerce.inventoryservice.processor;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.inventoryservice.entity.InventoryOutbox;
import com.ecommerce.inventoryservice.repository.InventoryOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryOutboxProcessor {

	private final InventoryOutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String topic = "inventory-update-topic";

	@Scheduled(fixedDelay = 5000)
	@Transactional
	public void processOutbox() {
		List<InventoryOutbox> events = outboxRepository.findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();
		log.info("InventoryOutboxProcessor Events ", events);
		
		for (InventoryOutbox event : events) {
			try {
				kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
				event.setProcessedAt(Instant.now());
				outboxRepository.save(event);
				log.info("Successfully synced SKU {} to Kafka", event.getAggregateId());
			} catch (Exception e) {
				log.error("Failed to sync inventory event {}: {}", event.getId(), e.getMessage());
			}
		}
	}
}