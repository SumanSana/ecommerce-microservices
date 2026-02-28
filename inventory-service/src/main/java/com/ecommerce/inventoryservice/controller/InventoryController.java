package com.ecommerce.inventoryservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.inventoryservice.dto.StockAdjustmentRequest;
import com.ecommerce.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ekart/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	// For Warehouse Admin: Adjustment of physical stock
	@PostMapping("/adjust")
	public ResponseEntity<Void> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
		inventoryService.adjustStock(request);
		return ResponseEntity.ok().build();
	}

	// Availability Check
	@GetMapping("/{skuId}/availability")
	public ResponseEntity<Integer> getAvailability(@PathVariable String skuId) {
		int available = inventoryService.getAvailableQuantity(skuId);
		return ResponseEntity.ok(available);
	}

}