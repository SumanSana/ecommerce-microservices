package com.ecommerce.productservice.listener;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dto.InventoryUpdateEvent;
import com.ecommerce.productservice.dto.ProductSyncEvent;
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
	public void consumeProductEvent(ProductSyncEvent product) {
		log.info("Received sync event: {}", product);

		try {

			ProductView view = ProductView.builder().skuId(product.skuId()).status(product.status())
					.productId(product.productId()).brandName(product.brandName()).categoryName(product.categoryName())
					.name(product.name()).description(product.description()).price(product.price())
					.attributes(product.attributes()).build();

			productViewRepository.save(view);

		} catch (Exception e) {
			log.error("Retrying: Sync failed for ID {}", product.productId());
			throw e;
		}
	}

	@KafkaListener(topics = "inventory-update-topic", containerFactory = "inventoryUpdate")
	public void handleInventoryUpdate(InventoryUpdateEvent event) {

		log.info("Received sync event from Inventory : {}", event);
		try {
			productViewRepository.findBySkuId(event.skuId()).ifPresent(view -> {

				view.setAvailableQuantity(event.availableQuantity());
				view.setInStock(event.availableQuantity() > 0);

				if (event.availableQuantity() == 0) {
					view.setStockLabel("Out of Stock");
				} else if (event.availableQuantity() > 0 && event.availableQuantity() < 10) {
					view.setStockLabel("Only " + event.availableQuantity() + " left!");
				} else {
					view.setStockLabel("In Stock");
				}
				productViewRepository.save(view);
			});

		} catch (Exception e) {
			log.error("Retrying: Sync failed for skuId : {}", event.skuId());
			throw e;
		}

	}

}