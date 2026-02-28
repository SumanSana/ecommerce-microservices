package com.ecommerce.productservice.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductMetadataRequest;
import com.ecommerce.productservice.dto.VariantUpdateLevelRequest;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductVariant;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.OutboxRepository;
import com.ecommerce.productservice.repository.ProductRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCommandService {

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final OutboxRepository outboxRepository;

	@Transactional
	public UUID createProduct(CreateProductRequest request) {
		Brand brand = brandRepository.findById(request.brandId()).orElseThrow();
		Category category = categoryRepository.findById(request.categoryId()).orElseThrow();

		Product product = new Product();
		product.setName(request.name());
		product.setDescription(request.description());
		product.setBrand(brand);
		product.setCategory(category);

		// Link variants properly for JPA
		List<ProductVariant> variants = request.variants().stream().map(v -> {
			ProductVariant variant = new ProductVariant();
			variant.setSkuId(v.skuId());
			variant.setPrice(v.price());
			variant.setAttributes(v.attributes());
			variant.setProduct(product);
			return variant;
		}).toList();

		product.setVariants(variants);
		productRepository.save(product);

		// Emit one event per variant for Mongo & Inventory sync
		for (ProductVariant v : variants) {
			emitOutboxEvent("PRODUCT_CREATED", v.getSkuId(),
					Map.of("productId", product.getId().toString(), "skuId", v.getSkuId(), "name", product.getName(),
							"description", product.getDescription(), "brandName", brand.getName(), "categoryName",
							category.getName(), "status", v.getStatus(), "price", v.getPrice(), "attributes", v.getAttributes(), "timestamp",
							System.currentTimeMillis()));
		}
		return product.getId();
	}

	@Transactional
	public void updateVariant(String skuId, VariantUpdateLevelRequest request) {

		emitOutboxEvent("VARIANT_UPDATED", skuId, Map.of("skuId", skuId, "price", request.price(), "attributes",
				request.attributes(), "timestamp", System.currentTimeMillis()));
	}

	private void emitOutboxEvent(String type, String aggregateId, Map<String, Object> payload) {
		OutboxEvent event = new OutboxEvent();
		event.setAggregateType("PRODUCT_SKU");
		event.setAggregateId(aggregateId);
		event.setType(type);
		event.setPayload(payload);
		outboxRepository.save(event);
	}

	public void updateProductMetadata(UUID id, @Valid ProductMetadataRequest request) {
		// TODO Auto-generated method stub

		Product product = productRepository.findById(id).orElseThrow();
		product.setName(request.name());
		product.setDescription(request.description());
		productRepository.save(product);

	}
}