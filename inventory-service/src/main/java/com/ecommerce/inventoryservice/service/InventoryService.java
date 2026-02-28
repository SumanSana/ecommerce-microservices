package com.ecommerce.inventoryservice.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.inventoryservice.dto.OrderEvent;
import com.ecommerce.inventoryservice.dto.OrderItem;
import com.ecommerce.inventoryservice.dto.StockAdjustmentRequest;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.InventoryOutbox;
import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.repository.InventoryOutboxRepository;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

	private final InventoryRepository inventoryRepository;
	private final InventoryTransactionRepository transactionRepository;
	private final InventoryOutboxRepository outboxRepository;

	@Transactional
	public void initializeInventory(String skuId) {
		if (!inventoryRepository.existsBySkuId(skuId)) {
			log.info("Initializing inventory record for new SKU: {}", skuId);
			Inventory inventory = new Inventory();
			inventory.setSkuId(skuId);
			inventory.setTotalQuantity(0);
			inventory.setReservedQuantity(0);
			inventoryRepository.save(inventory);
			recordTransaction(skuId, 0, "INITIALIZATION", "SKU_INIT", skuId);

		} else {
			log.warn("Attempted to initialize existing SKU: {}", skuId);
		}
	}

	/**
	 * Warehourse Adjustment - By Shop-Owner
	 */

	public void adjustStock(StockAdjustmentRequest request) {

		Inventory inventory = inventoryRepository.findBySkuId(request.skuId())
				.orElseThrow(() -> new RuntimeException("Skuid not found: " + request.skuId()));

		int curQuantity = inventory.getTotalQuantity();
		int updatedQuantity = request.transactionType().equals("INBOUND") ? curQuantity + request.adjustment()
				: curQuantity - request.adjustment();
		inventory.setTotalQuantity(updatedQuantity);
		inventoryRepository.save(inventory);
		recordTransaction(request.skuId(), request.adjustment(), request.transactionType(), "SKU_ADJUSTMENT",
				request.skuId());
	}

	/**
	 * ATOMIC RESERVATION (Order Created)
	 */
	@Transactional
	public void reserveStock(OrderEvent event) {
		if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_RESERVED"))
			return;

		for (OrderItem item : event.items()) {
			int updatedRows = inventoryRepository.reserveStockAtomic(item.skuId(), item.quantity());
			if (updatedRows == 0) {
				log.warn("Insufficient derived availability for SKU: {}", item.skuId());
				throw new RuntimeException("Insufficient stock for SKU: " + item.skuId());
			}
			recordTransaction(item.skuId(), item.quantity(), "OUTBOUND", "ORDER_RESERVED", event.orderId().toString());
		}
	}

	/**
	 * COMMIT - Order Confirmed
	 */
	@Transactional
	public void commitStock(OrderEvent event) {
		if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CONFIRMED"))
			return;
		for (OrderItem item : event.items()) {
			inventoryRepository.commitStock(item.skuId(), item.quantity());
			recordTransaction(item.skuId(), item.quantity(), "OUTBOUND", "ORDER_CONFIRMED", event.orderId().toString());
		}
	}

	/**
	 * RELEASE - Order Cancelled
	 */
	@Transactional
	public void releaseStock(OrderEvent event) {
		if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CANCELLED"))
			return;
		if (!transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_RESERVED"))
			return;
		if(transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CONFIRMED")) {
			for (OrderItem item : event.items()) {
				inventoryRepository.rollbackInventoryForCancelledOrder(item.skuId(), item.quantity());
				recordTransaction(item.skuId(), item.quantity(), "INBOUND", "ORDER_CANCELLED", event.orderId().toString());
			}
		}
		else {
			for (OrderItem item : event.items()) {
				inventoryRepository.releaseStock(item.skuId(), item.quantity());
				recordTransaction(item.skuId(), item.quantity(), "INBOUND", "ORDER_CANCELLED", event.orderId().toString());
			}
		}
		
	}

	/**
	 * AVAILABILITY CHECK (Derived)
	 */
	@Transactional(readOnly = true)
	public int getAvailableQuantity(String skuId) {
		return inventoryRepository.findBySkuId(skuId).map(inv -> inv.getTotalQuantity() - inv.getReservedQuantity())
				.orElse(0);
	}

	private void recordTransaction(String skuId, int qty, String transactionType, String refType, String refId) {
		Inventory inventory = inventoryRepository.findBySkuId(skuId).orElseThrow();

		// Audit Trail
		InventoryTransaction tx = new InventoryTransaction();
		tx.setSkuId(skuId);
		tx.setInventory(inventory);
		tx.setTransactionType(transactionType);
		tx.setReferenceType(refType);
		tx.setReferenceId(refId);
		tx.setQuantityChanged(qty);
		transactionRepository.save(tx);

		if (!refType.equals("INITIALIZATION")) {
			int currentAvailable = inventory.getTotalQuantity() - inventory.getReservedQuantity();
			saveOutbox(skuId, "INVENTORY_UPDATE", refType, currentAvailable);
		}
	}

	private void saveOutbox(String skuId, String eventType, String refType, int available) {
		InventoryOutbox outbox = new InventoryOutbox();
		outbox.setAggregateId(skuId);
		outbox.setEventType(eventType);
		outbox.setAggregateType(refType);
		outbox.setPayload(
				Map.of("skuId", skuId, "availableQuantity", available, "updatedAt", Instant.now().toString()));
		outboxRepository.save(outbox);
	}

}