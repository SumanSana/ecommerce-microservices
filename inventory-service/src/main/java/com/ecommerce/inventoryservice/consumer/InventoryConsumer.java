package com.ecommerce.inventoryservice.consumer;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.inventoryservice.dto.InventoryResponseEvent;
import com.ecommerce.inventoryservice.dto.OrderEvent;
import com.ecommerce.inventoryservice.dto.ProductInitEvent;
import com.ecommerce.inventoryservice.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {

	private final InventoryService inventoryService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@KafkaListener(topics = "inventory-init-topic", containerFactory="productInit")
	public void handleProductCreated(ProductInitEvent product) {
		log.info("Received payload: {}", product);

		String skuId = product.skuId();
		log.info("Received product created event for SKU: {}", skuId);
		inventoryService.initializeInventory(skuId);
	}

	@KafkaListener(topics = "order-events-topic", containerFactory="orderEvent")
	public void handleOrderEvent(OrderEvent event) {
		String status = event.status();
		log.info("Inventory: Processing {} for Order {}", status, event.orderId());

		try {
			switch (status) {
			case "PENDING" -> {
				inventoryService.reserveStock(event);
				publishStatus(event.orderId(), true, "Success");
			}
			case "CONFIRMED" -> {
				inventoryService.commitStock(event);
			}
			case "CANCELLED" -> {
				inventoryService.releaseStock(event);
			}
			}
		} catch (Exception e) {
			log.error("Inventory: Technical error for Order {}: {}", event.orderId(), e.getMessage());
			if ("CREATED".equals(status)) {
				publishStatus(event.orderId(), false, "Business Logic Failure: " + e.getMessage());
			} else {
				throw e;
			}
		}
	}

	private void publishStatus(UUID orderId, Boolean status, String message) {
		InventoryResponseEvent response = new InventoryResponseEvent(orderId, status, message);
		kafkaTemplate.send("inventory-response-topic", orderId.toString(), response);
	}
}