package com.ecommerce.productservice.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dto.InventoryUpdateEvent;
import com.ecommerce.productservice.entity.ProductView;
import com.ecommerce.productservice.repository.ProductViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncConsumer {

	private final ProductViewRepository productViewRepository;

	@KafkaListener(topics = "product-sync-topic", containerFactory = "productSync")
	public void consumeProductEvent(ProductView product) {
		log.info("Received sync event: {}", product);

		try {
			productViewRepository.save(product);

		} catch (Exception e) {
			log.error("Retrying: Sync failed for ID {}", product.getProductId());
			throw e;
		}
	}

	@KafkaListener(topics = "inventory-update-topic", containerFactory = "inventoryUpdate")
	public void handleInventoryUpdate(InventoryUpdateEvent event) {

		log.info("Received sync event from Inventory : {}", event);
		try {
			productViewRepository.findBySkuId(event.skuId()).ifPresent(view -> {
				productViewRepository.updateInventory(event.skuId(), event.availableQuantity());
			});

		} catch (Exception e) {
			log.error("Retrying: Sync failed for skuId : {}", event.skuId());
			throw e;
		}

	}

}