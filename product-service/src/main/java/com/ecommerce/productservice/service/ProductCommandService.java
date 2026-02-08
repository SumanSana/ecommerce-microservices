package com.ecommerce.productservice.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dto.ProductCreateRequest;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductVariant;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.OutboxRepository;
import com.ecommerce.productservice.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCommandService {

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final OutboxRepository outboxRepository;

	@Transactional
	public UUID createProduct(ProductCreateRequest request) {
		
		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new RuntimeException("Brand not found"));
		
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new RuntimeException("Category not found"));

		// 2. Create the Parent Product
		Product product = new Product();
		product.setName(request.name());
		product.setDescription(request.description());
		product.setBrand(brand);
		product.setCategory(category);

		// 3. Create the Variant (SKU)
		ProductVariant variant = new ProductVariant();
		variant.setProduct(product);
		variant.setSkuId(request.skuId());
		variant.setPrice(request.price());
		variant.setAttributes(request.attributes());

		product.setVariants(java.util.List.of(variant));
		productRepository.save(product);

		// 4. Create the Outbox Event for Kafka/MongoDB sync
		OutboxEvent outboxEvent = new OutboxEvent();
		outboxEvent.setAggregateType("PRODUCT");
		outboxEvent.setAggregateId(product.getId().toString());
		outboxEvent.setType("PRODUCT_CREATED");

		// Prepare the payload for MongoDB synchronization
		outboxEvent.setPayload(Map.of("id", product.getId().toString(), "name", product.getName(), "skuId",
				variant.getSkuId(), "price", variant.getPrice(), "brandName", brand.getName(), "categoryName",
				category.getName(), "attributes", variant.getAttributes()));

		outboxRepository.save(outboxEvent);

		return product.getId();
	}

	@Transactional
	public void updateProduct(UUID productId, ProductCreateRequest request) {
		
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		// Will allow update for one product variant at a time as of now
		ProductVariant variant = product.getVariants().stream().filter(v -> v.getSkuId().equals(request.skuId()))
				.findFirst().orElseThrow(() -> new RuntimeException("SKU not found"));

		product.setName(request.name());
		product.setDescription(request.description());
		variant.setPrice(request.price());
		variant.setAttributes(request.attributes());

		productRepository.save(product);


		OutboxEvent outboxEvent = new OutboxEvent();
		outboxEvent.setAggregateType("PRODUCT");
		outboxEvent.setAggregateId(product.getId().toString());
		outboxEvent.setType("PRODUCT_UPDATED");

		outboxEvent.setPayload(Map.of("id", product.getId().toString(), "name", product.getName(), "skuId",
				variant.getSkuId(), "price", variant.getPrice(), "brandName", product.getBrand().getName(),
				"categoryName", product.getCategory().getName(), "attributes", variant.getAttributes()));

		outboxRepository.save(outboxEvent);
	}
}