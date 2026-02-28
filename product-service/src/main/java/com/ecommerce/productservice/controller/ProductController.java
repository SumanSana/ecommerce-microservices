package com.ecommerce.productservice.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductMetadataRequest;
import com.ecommerce.productservice.dto.VariantUpdateLevelRequest;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.ProductView;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductViewRepository;
import com.ecommerce.productservice.service.ProductCommandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ekart/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductCommandService commandService;
	private final ProductViewRepository queryRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;

	@PostMapping("/brands")
	public ResponseEntity<Brand> createBrand(@RequestBody Brand brand) {
		return ResponseEntity.status(HttpStatus.CREATED).body(brandRepository.save(brand));
	}

	@PostMapping("/categories")
	public ResponseEntity<Category> createCategory(@RequestBody Category category) {
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
	}

	@PostMapping
	public ResponseEntity<UUID> createProduct(@Valid @RequestBody CreateProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commandService.createProduct(request));
	}

	@PatchMapping("/{id}/metadata")
	public ResponseEntity<Void> updateProductMetadata(@PathVariable UUID id,
			@Valid @RequestBody ProductMetadataRequest request) {
		commandService.updateProductMetadata(id, request);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/variants/{skuId}")
	public ResponseEntity<Void> updateVariant(@PathVariable String skuId,
			@Valid @RequestBody VariantUpdateLevelRequest request) {
		commandService.updateVariant(skuId, request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<Page<ProductView>> getProducts(@RequestParam(required = false) String brandName,
			@RequestParam(required = false) String categoryName, @RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice, @PageableDefault(size = 20) Pageable pageable) {

		// This hits MongoDB - extremely fast even with millions of products
		return ResponseEntity.ok(queryRepository.findByFilters(brandName, categoryName, minPrice, maxPrice, pageable));
	}

	@GetMapping("/{skuId}")
	public ResponseEntity<ProductView> getProductBySku(@PathVariable String skuId) {
		return queryRepository.findBySkuId(skuId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/search")
	public ResponseEntity<Page<ProductView>> searchByName(@RequestParam String name,
			@PageableDefault(size = 10) Pageable pageable) {
		return ResponseEntity.ok(queryRepository.findByNameContainingIgnoreCase(name, pageable));
	}

}