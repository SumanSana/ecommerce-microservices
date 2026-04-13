package com.ecommerce.productservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductUpdateDTO;
import com.ecommerce.productservice.entity.Brand;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.repository.BrandRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.service.ProductCommandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ekart/v1/products/admin")
@RequiredArgsConstructor
public class ProductManagementController {

	private final ProductCommandService commandService;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;

	@PostMapping("/brands")
	public ResponseEntity<Brand> createBrand(@RequestBody Brand brand) {
		return ResponseEntity.status(HttpStatus.CREATED).body(brandRepository.save(brand));
	}

	@PostMapping("/categories")
	public ResponseEntity<Category> createCategory(@RequestBody Category category) {
		if (category.getSubCategories() != null) {
			category.getSubCategories().forEach(sub -> sub.setParent(category));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UUID> createProduct(@RequestPart("product") CreateProductRequest request,
			@RequestPart("images") List<MultipartFile> images) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commandService.createProduct(request, images));
	}

	@PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<UUID> updateProduct(@PathVariable UUID id, @RequestPart("request") ProductUpdateDTO request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) {

		UUID updatedId = commandService.updateProduct(id, request, images);
		return ResponseEntity.ok(updatedId);
	}

}
