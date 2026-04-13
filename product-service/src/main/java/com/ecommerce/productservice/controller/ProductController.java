package com.ecommerce.productservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerce.productservice.dto.ProductSearchCriteria;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.ProductView;
import com.ecommerce.productservice.entity.VariantView;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductVariantRepository;
import com.ecommerce.productservice.repository.ProductViewRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ekart/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductViewRepository queryRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantRepository productVariantRepository;

	@GetMapping("/brands")
	public ResponseEntity<List<Brand>> getBrands() {
		return ResponseEntity.status(HttpStatus.OK).body(brandRepository.findAll());
	}

	@GetMapping("/categories")
	public ResponseEntity<List<Category>> getCategories() {
		return ResponseEntity.status(HttpStatus.OK).body(categoryRepository.findAll());
	}

	@GetMapping("/leaf-categories")
	public ResponseEntity<List<Category>> getLeafCategories() {
		return ResponseEntity.status(HttpStatus.OK).body(categoryRepository.findAllLeafCategories());
	}

	@GetMapping
	public ResponseEntity<List<ProductView>> getAllProducts() {
		return ResponseEntity.ok(queryRepository.findAll());
	}

	@GetMapping("/active")
	public ResponseEntity<List<ProductView>> getAllActiveProducts() {
		return ResponseEntity.ok(queryRepository.findAllActiveProductsAndVariants());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductView> getProduct(@PathVariable String id) {
		ProductView product = queryRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Product not found with the given Id: " + id));
		return ResponseEntity.ok(product);
	}

	@GetMapping("/search")
	public ResponseEntity<Page<ProductView>> searchProducts(@Valid ProductSearchCriteria criteria,
			@PageableDefault(size = 20, sort = "productId") Pageable pageable) {

		if (!criteria.isValidPriceRange()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must be <= maxPrice");
		}

		// 1. Get the page from your repo (which you said handles the other search
		// filters)
		Page<ProductView> productPage = queryRepository.findByFilters(criteria, pageable);

		// 2. THE CLEANUP
		List<ProductView> filteredContent = productPage.getContent().stream()
				// STEP A: CHECK PRODUCT STATUS FIRST
				.filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))

				// STEP B: FILTER VARIANTS ONLY FOR ACTIVE PRODUCTS
				.map(p -> {
					if (p.getVariants() != null) {
						List<VariantView> activeVariants = p.getVariants().stream()
								.filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus())).toList();
						p.setVariants(activeVariants);
					}
					return p;
				})

				// STEP C: DROP PRODUCTS THAT HAVE NO ACTIVE VARIANTS LEFT
				.filter(p -> p.getVariants() != null && !p.getVariants().isEmpty()).toList();

		return ResponseEntity.ok(new PageImpl<>(filteredContent, pageable, productPage.getTotalElements()));
	}

	@GetMapping("/variants/{skuId}")
	public ResponseEntity<ProductView> getProductBySku(@PathVariable String skuId) {
		return queryRepository.findBySkuId(skuId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/variants/{skuId}/price")
	public BigDecimal getSkuPrice(@PathVariable String skuId) {
		return productVariantRepository.findPriceBySkuId(skuId);
	}

}