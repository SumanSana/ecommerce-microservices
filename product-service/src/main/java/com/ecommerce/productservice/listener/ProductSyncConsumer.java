package com.ecommerce.productservice.listener;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.entity.ProductView;
import com.ecommerce.productservice.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncConsumer {

	private final ProductViewRepository productViewRepository;

	@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000, multiplier = 2.0), topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE, dltStrategy = DltStrategy.FAIL_ON_ERROR)
	@KafkaListener(topics = "product-sync-topic", groupId = "product-group")
	public void consumeProductEvent(Map<String, Object> payload) {
		log.info("Received sync event: {}", payload);

		try {

			String id = payload.get("id").toString();

			ProductView view = ProductView.builder().id(id).skuId(payload.get("skuId").toString())
					.name(payload.get("name").toString()).brandName(payload.get("brandName").toString())
					.price(new BigDecimal(payload.get("price").toString()))
					.attributes((Map<String, Object>) payload.get("attributes")).build();

			productViewRepository.save(view);

		} catch (Exception e) {
			log.error("Retrying: Sync failed for ID {}", payload.get("id"));
			throw e;
		}
	}

	@DltHandler
	public void handleDlt(Map<String, Object> payload) {
		log.error("CRITICAL: Message moved to DLT after multiple retries: {}", payload);
	}
}