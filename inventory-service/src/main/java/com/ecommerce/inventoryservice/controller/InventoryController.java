package com.ecommerce.inventoryservice.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.inventoryservice.dto.StockAdjustmentRequest;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ekart/v1/inventory/admin")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;
	private final InventoryRepository inventoryRepository;

	// For Warehouse Admin: Adjustment of physical stock
	@PostMapping("/transactions")
	public ResponseEntity<Void> updateStock(@Valid @RequestBody StockAdjustmentRequest request) {
		inventoryService.adjustStock(request);
		return ResponseEntity.ok().build();
	}

	// SKU Search
	@GetMapping("/skus/search")
	public ResponseEntity<List<String>> searchSkus(@RequestParam(required = false) String term) {
		Pageable limit = PageRequest.of(0, 100);

		List<String> skuIds;

		if (term == null || term.isBlank()) {
			skuIds = inventoryRepository.findAll(limit).getContent().stream().map(Inventory::getSkuId).toList();
		} else {
			skuIds = inventoryRepository.findBySkuIdContainingIgnoreCase(term, limit).stream().map(Inventory::getSkuId).toList();
		}

		return ResponseEntity.ok(skuIds);
	}

	// Availability Check
	@GetMapping("/{skuId}/availability")
	public ResponseEntity<Integer> getAvailability(@PathVariable String skuId) {
		int available = inventoryService.getAvailableQuantity(skuId);
		return ResponseEntity.ok(available);
	}

}