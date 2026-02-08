package com.ecommerce.productservice.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dto.ProductCreateRequest;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductCommandService commandService;
	private final ProductViewRepository queryRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;

	@PostMapping("/brand")
	public ResponseEntity<Brand> createBrand(@RequestBody Brand request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(brandRepository.save(request));
	}

	@PostMapping("/category")
	public ResponseEntity<Category> createCategory(@RequestBody Category request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(request));
	}

	@PostMapping
	public ResponseEntity<UUID> createProduct(@Valid @RequestBody ProductCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commandService.createProduct(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Void> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductCreateRequest request) {
		commandService.updateProduct(id, request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping()
	public ResponseEntity<Page<ProductView>> getProducts(@RequestParam(required = false) String brandName,
			@RequestParam(required = false) String categoryName, @RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice, @PageableDefault(size = 20) Pageable pageable) {

		return ResponseEntity.ok(queryRepository.findByFilters(brandName, categoryName, minPrice, maxPrice, pageable));
	}

	@GetMapping("/search")
	public ResponseEntity<Page<ProductView>> searchByName(@RequestParam String name,
			@PageableDefault(size = 10) Pageable pageable) {
		return ResponseEntity.ok(queryRepository.findByNameContainingIgnoreCase(name, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductView> getProductById(@PathVariable String id) {
		return queryRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}
}