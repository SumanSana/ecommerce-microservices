package com.ecommerce.inventoryservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
	private final RedissonClient redissonClient; // Added Redisson

	@Transactional
	public void initializeInventory(String skuId) {
		// Use a simple lock for single SKU operations
		withSingleLock(skuId, () -> {
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
		});
	}

	/**
	 * Warehouse Adjustment - By Shop-Owner
	 */
	@Transactional // Added missing Transactional annotation
	public void adjustStock(StockAdjustmentRequest request) {
		withSingleLock(request.skuId(), () -> {
			Inventory inventory = inventoryRepository.findBySkuId(request.skuId())
					.orElseThrow(() -> new RuntimeException("Skuid not found: " + request.skuId()));

			int curQuantity = inventory.getTotalQuantity();
			int updatedQuantity = request.transactionType().equals("INBOUND") ? curQuantity + request.adjustment()
					: curQuantity - request.adjustment();
			
			inventory.setTotalQuantity(updatedQuantity);
			inventoryRepository.save(inventory);
			recordTransaction(request.skuId(), request.adjustment(), request.transactionType(), "SKU_ADJUSTMENT",
					request.skuId());
		});
	}

	/**
	 * ATOMIC RESERVATION (Order Created)
	 */
	@Transactional
	public void reserveStock(OrderEvent event) {
		withMultiLock(event, () -> {
			// Idempotency check happens INSIDE the lock safely
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
		});
	}

	/**
	 * COMMIT - Order Confirmed
	 */
	@Transactional
	public void commitStock(OrderEvent event) {
		withMultiLock(event, () -> {
			if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CONFIRMED"))
				return;
				
			for (OrderItem item : event.items()) {
				inventoryRepository.commitStock(item.skuId(), item.quantity());
				recordTransaction(item.skuId(), item.quantity(), "OUTBOUND", "ORDER_CONFIRMED", event.orderId().toString());
			}
		});
	}

	/**
	 * RELEASE - Order Cancelled
	 */
	@Transactional
	public void releaseStock(OrderEvent event) {
		withMultiLock(event, () -> {
			if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CANCELLED"))
				return;
			if (!transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_RESERVED"))
				return;
				
			if (transactionRepository.existsByReferenceIdAndReferenceType(event.orderId().toString(), "ORDER_CONFIRMED")) {
				for (OrderItem item : event.items()) {
					inventoryRepository.rollbackInventoryForCancelledOrder(item.skuId(), item.quantity());
					recordTransaction(item.skuId(), item.quantity(), "INBOUND", "ORDER_CANCELLED", event.orderId().toString());
				}
			} else {
				for (OrderItem item : event.items()) {
					inventoryRepository.releaseStock(item.skuId(), item.quantity());
					recordTransaction(item.skuId(), item.quantity(), "INBOUND", "ORDER_CANCELLED", event.orderId().toString());
				}
			}
		});
	}

	/**
	 * AVAILABILITY CHECK (Derived)
	 * No lock needed here, dirty reads are fine for display purposes.
	 */
	@Transactional(readOnly = true)
	public int getAvailableQuantity(String skuId) {
		return inventoryRepository.findBySkuId(skuId).map(inv -> inv.getTotalQuantity() - inv.getReservedQuantity())
				.orElse(0);
	}

	// ==========================================
	// HELPER METHODS
	// ==========================================

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

	/**
	 * Redis MultiLock Wrapper (Sorted to prevent deadlocks)
	 */
	private void withMultiLock(OrderEvent event, Runnable action) {
		// 1. Sort SKUs lexicographically to prevent deadlocks
		List<String> sortedSkus = event.items().stream()
				.map(OrderItem::skuId)
				.distinct()
				.sorted()
				.toList();

		// 2. Prepare locks
		RLock[] locks = sortedSkus.stream()
				.map(sku -> redissonClient.getLock("lock:inventory:" + sku))
				.toArray(RLock[]::new);

		RLock multiLock = redissonClient.getMultiLock(locks);
		boolean isLocked = false;

		try {
			// 3. Try to acquire lock (Wait 5s, auto-release after 10s)
			isLocked = multiLock.tryLock(5, 10, TimeUnit.SECONDS);
			if (!isLocked) {
				log.error("Failed to acquire MultiLock for Order: {}", event.orderId());
				throw new RuntimeException("System busy processing inventory. Please try again.");
			}
			
			// 4. Execute the DB logic
			action.run();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for inventory lock", e);
		} finally {
			// 5. Always release
			if (isLocked) {
				multiLock.unlock();
			}
		}
	}

	/**
	 * Redis Single Lock Wrapper (For single item adjustments)
	 */
	private void withSingleLock(String skuId, Runnable action) {
		RLock lock = redissonClient.getLock("lock:inventory:" + skuId);
		boolean isLocked = false;

		try {
			isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
			if (!isLocked) {
				log.error("Failed to acquire lock for SKU: {}", skuId);
				throw new RuntimeException("System busy processing SKU: " + skuId);
			}
			
			action.run();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for lock", e);
		} finally {
			if (isLocked) {
				lock.unlock();
			}
		}
	}
}