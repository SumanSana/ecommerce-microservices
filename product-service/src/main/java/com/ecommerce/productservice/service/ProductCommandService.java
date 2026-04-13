package com.ecommerce.productservice.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductUpdateDTO;
import com.ecommerce.productservice.dto.VariantRequest;
import com.ecommerce.productservice.dto.VariantUpdateDTO;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.OutboxEvent;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductVariant;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.OutboxRepository;
import com.ecommerce.productservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

@Service
@RequiredArgsConstructor
public class ProductCommandService {

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final OutboxRepository outboxRepository;
	private final S3Service s3Service;

	@Transactional
	public UUID createProduct(CreateProductRequest request, List<MultipartFile> images) {
		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new RuntimeException("Brand not found"));
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new RuntimeException("Category not found"));

		Product product = new Product();
		product.setName(request.name());
		product.setDescription(request.description());
		product.setBrand(brand);
		product.setCategory(category);
		product.setStatus("ACTIVE");

		List<ProductVariant> variants = new ArrayList<>();

		for (int i = 0; i < request.variants().size(); i++) {
			VariantRequest vReq = request.variants().get(i);
			MultipartFile file = (images != null && i < images.size()) ? images.get(i) : null;

			if (file == null || file.isEmpty()) {
				throw new RuntimeException("Image is mandatory for product creation: " + vReq.skuId());
			}

			String s3Url = processAndUpload(file, brand.getName(), vReq.skuId());
			ProductVariant variant = new ProductVariant();
			variant.setSkuId(vReq.skuId());
			variant.setPrice(vReq.price());
			variant.setStatus("ACTIVE");
			variant.setAttributes(vReq.attributes());
			variant.setImageUrl(s3Url);
			variant.setProduct(product);
			variants.add(variant);
		}

		product.setVariants(variants);
		productRepository.save(product);

		// 4. Outbox Event
		emitOutboxEvent("PRODUCT_CREATED", "NEW_PRODUCT_ADDED", product.getId().toString(),
				Map.of("productId", product.getId().toString(), "brandName", brand.getName(),
						"categoryName", category.getName(), "name", product.getName(), "description", product.getDescription(), "status", product.getStatus(), "variants", variants, "timestamp",
						System.currentTimeMillis()));

		return product.getId();
	}

	@Transactional
	public UUID updateProduct(UUID id, ProductUpdateDTO request, List<MultipartFile> images) {
		// 1. Fetch existing product
		Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new RuntimeException("Brand not found"));
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new RuntimeException("Category not found"));
		
		// 2. Update Product Metadata
		product.setName(request.name());
		product.setDescription(request.description());
		product.setStatus(request.status());
		product.setBrand(brand);
		product.setCategory(category);

		// 3. Prepare for Variant Syncing
		Map<String, ProductVariant> existingVariantsMap = product.getVariants().stream()
				.collect(Collectors.toMap(ProductVariant::getSkuId, v -> v));

		List<ProductVariant> updatedVariants = new ArrayList<>();

		for (int i = 0; i < request.variants().size(); i++) {
			VariantUpdateDTO vDto = request.variants().get(i);
			// Check if a file was actually uploaded for this specific index
			MultipartFile file = (images != null && i < images.size()) ? images.get(i) : null;

			ProductVariant variant = existingVariantsMap.get(vDto.skuId());

			String finalImageUrl;

			// 4. Image Logic: Upload new or keep old?
			if (file != null && !file.isEmpty()) {
				// A new file was provided -> Process & Upload to S3
				finalImageUrl = processAndUpload(file, product.getBrand().getName(), vDto.skuId());
			} else if (variant != null) {
				// No new file, but variant exists -> Keep the existing image
				finalImageUrl = variant.getImageUrl();
			} else {
				// New variant but NO image provided -> Throw error (or use placeholder)
				throw new RuntimeException("Image is required for new variant: " + vDto.skuId());
			}

			if (variant != null) {
				// UPDATE EXISTING VARIANT
				variant.setPrice(vDto.price());
				variant.setStatus(vDto.status());
				variant.setAttributes(vDto.attributes());
				variant.setImageUrl(finalImageUrl);
				updatedVariants.add(variant);
			} else {
				// CREATE NEW VARIANT
				ProductVariant newVariant = new ProductVariant();
				newVariant.setSkuId(vDto.skuId());
				newVariant.setPrice(vDto.price());
				newVariant.setStatus(vDto.status());
				newVariant.setAttributes(vDto.attributes());
				newVariant.setImageUrl(finalImageUrl);
				newVariant.setProduct(product);
				updatedVariants.add(newVariant);
			}
		}

		// 5. Sync and Save (Orphan Removal handles deletions)
		product.getVariants().clear();
		product.getVariants().addAll(updatedVariants);
		productRepository.save(product);

		// 6. Emit Update Event (for Search/Inventory services)
		emitOutboxEvent("PRODUCT_UPDATED", "PRODUCT_INFO_CHANGED", product.getId().toString(),
				Map.of("productId", product.getId().toString(), "brandName", product.getBrand().getName(),
						"categoryName", product.getCategory().getName(), "name", product.getName(), "description", product.getDescription(), "status", product.getStatus(), "variants", product.getVariants(), "timestamp",
						System.currentTimeMillis()));
		
		return product.getId();
	}

	/**
	 * Helper to keep code DRY - Reuse your compression and S3 logic
	 */
	private String processAndUpload(MultipartFile file, String brandName, String skuId) {
		try {
			String extension = ".jpg"; // You can extract this from file.getOriginalFilename()
			String newFileName = brandName.toLowerCase().replaceAll("\\s+", "") + "_"
					+ skuId.toLowerCase().replaceAll("\\s+", "") + extension;

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Thumbnails.of(file.getInputStream()).size(800, 800).outputFormat("jpg").outputQuality(0.75)
					.toOutputStream(outputStream);

			return s3Service.uploadByteArray(outputStream.toByteArray(), newFileName, "image/jpeg");
		} catch (IOException e) {
			throw new RuntimeException("Failed to process image for SKU: " + skuId, e);
		}
	}

	private void emitOutboxEvent(String eventType, String refType, String aggregateId, Map<String, Object> payload) {

		OutboxEvent event = new OutboxEvent();
		event.setType(eventType);
		event.setAggregateType(refType);
		event.setAggregateId(aggregateId);
		event.setPayload(payload);
		outboxRepository.save(event);
	}
}