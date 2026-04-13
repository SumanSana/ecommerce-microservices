package com.ecommerce.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.Address;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderEvent;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.entity.OutboxEvent;
import com.ecommerce.orderservice.repository.OrderEventRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderEventRepository orderEventRepository;
	private final OutboxRepository outboxRepository;
	private final ProductClient productClient;
	private final PaymentClient paymentClient;
	private final RedissonClient redissonClient;

	/**
	 * Creates an order. Uses a "Fail-Fast" lock to prevent double-click
	 * submissions.
	 */
	@Transactional
	public OrderResponse createOrder(OrderRequest request) {
		// Key: unique to the customer to prevent duplicate clicks
		String lockKey = "lock:order-create:" + request.customerId();

		// We use 0 wait time here because we want to fail immediately if a second click
		// happens
		return withLock(lockKey, 0, 15, () -> {
			log.info("Creating order for customer: {}", request.customerId());

			List<OrderItem> orderItems = request.items().stream().map(itemReq -> {
				BigDecimal price = productClient.getSkuPrice(itemReq.skuId());
				return OrderItem.builder().skuId(itemReq.skuId()).quantity(itemReq.quantity()).price(price).build();
			}).toList();

			BigDecimal totalAmount = orderItems.stream()
					.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			Order order = Order.builder().customerId(request.customerId()).customerEmail(request.customerEmail())
					.status(OrderStatus.PENDING).totalAmount(totalAmount)
					.shippingAddressLine1(request.shippingAddressLine1()).shippingCity(request.shippingCity())
					.shippingZipCode(request.shippingZipCode()).shippingCountry(request.shippingCountry())
					.items(orderItems).build();

			Order savedOrder = orderRepository.save(order);

			PaymentRequest paymentRequest = new PaymentRequest(savedOrder.getId(), savedOrder.getCustomerEmail(),
					savedOrder.getTotalAmount());
			Map<String, String> paymentResponse = paymentClient.initiate(paymentRequest);

			saveEventLog(savedOrder.getId(), "ORDER", "PENDING");

			saveToOutbox("ORDER", savedOrder.getId().toString(), "PENDING",
					Map.of("orderId", savedOrder.getId(), "customerId", savedOrder.getCustomerId(), "customerEmail",
							savedOrder.getCustomerEmail(), "totalAmount", savedOrder.getTotalAmount(), "status",
							savedOrder.getStatus(), "items", savedOrder.getItems()));

			return new OrderResponse(savedOrder.getId(), "PENDING", totalAmount, paymentResponse.get("clientSecret"));
		});
	}

	@Transactional
	public void handleInventoryResponse(UUID orderId, boolean success) {
		withLock("lock:order-process:" + orderId, 5, 10, () -> {
			saveEventLog(orderId, "INVENTORY", success ? "SUCCESS" : "FAILURE");
			verifyAndProcessOrder(orderId);
		});
	}

	@Transactional
	public void handlePaymentResponse(UUID orderId, String status) {
		withLock("lock:order-process:" + orderId, 5, 10, () -> {
			boolean success = "SUCCESS".equalsIgnoreCase(status);
			saveEventLog(orderId, "PAYMENT", success ? "SUCCESS" : "FAILURE");
			verifyAndProcessOrder(orderId);
		});
	}

	@Transactional
	public void cancelOrder(UUID id) {
		// No lock needed here as it's usually called inside an already locked method
		// (verifyAndProcessOrder)
		// Or we can add it for safety if called directly
		Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order id not found: " + id));
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		saveEventLog(order.getId(), "ORDER", "CANCELLED");

		saveToOutbox("ORDER", order.getId().toString(), "CANCELLED",
				Map.of("orderId", order.getId(), "customerEmail", order.getCustomerEmail(), "totalAmount",
						order.getTotalAmount(), "status", order.getStatus(), "items", order.getItems()));

		saveToOutbox("PAYMENT", order.getId().toString(), "REFUND", Map.of("orderId", order.getId(), "customerEmail",
				order.getCustomerEmail(), "amount", order.getTotalAmount()));
	}

	@Transactional
	public void updateShippingAddress(UUID orderId, Address newAddress) {
		withLock("lock:order-process:" + orderId, 5, 10, () -> {
			Order order = orderRepository.findById(orderId)
					.orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));

			if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED
					|| order.getStatus() == OrderStatus.CANCELLED) {
				throw new IllegalStateException("Address cannot be updated for order in status: " + order.getStatus());
			}

			order.setShippingAddressLine1(newAddress.shippingAddressLine1());
			order.setShippingCity(newAddress.shippingCity());
			order.setShippingZipCode(newAddress.shippingZipCode());
			order.setShippingCountry(newAddress.shippingCountry());
			orderRepository.save(order);
			saveEventLog(order.getId(), "SHIPPING-ADDRESS", "UPDATED");
			log.info("Address updated for Order: {}", orderId);
		});
	}

	/**
	 * Logic to check if both Inventory and Payment are received.
	 */
	private void verifyAndProcessOrder(UUID orderId) {
		List<OrderEvent> events = orderEventRepository.findByOrderId(orderId);
		Order order = orderRepository.findById(orderId).orElseThrow();

		if (order.getStatus() != OrderStatus.PENDING)
			return;

		Optional<OrderEvent> invEvent = events.stream().filter(e -> "INVENTORY".equals(e.getEventType())).findFirst();
		Optional<OrderEvent> payEvent = events.stream().filter(e -> "PAYMENT".equals(e.getEventType())).findFirst();

		if ((invEvent.isPresent() && "FAILURE".equals(invEvent.get().getStatus()))
				|| (payEvent.isPresent() && "FAILURE".equals(payEvent.get().getStatus()))) {
			cancelOrder(order.getId());
			return;
		}

		if (invEvent.isPresent() && "SUCCESS".equals(invEvent.get().getStatus()) && payEvent.isPresent()
				&& "SUCCESS".equals(payEvent.get().getStatus())) {

			order.setStatus(OrderStatus.CONFIRMED);
			orderRepository.save(order);
			saveEventLog(order.getId(), "ORDER", "CONFIRMED");

			saveToOutbox("ORDER", order.getId().toString(), "CONFIRMED",
					Map.of("orderId", order.getId(), "customerId", order.getCustomerId(), "customerEmail",
							order.getCustomerEmail(), "totalAmount", order.getTotalAmount(), "status",
							order.getStatus(), "items", order.getItems()));
		}
	}

	private void saveEventLog(UUID orderId, String type, String status) {
		OrderEvent event = orderEventRepository.findByOrderIdAndEventType(orderId, type)
				.orElseGet(() -> OrderEvent.builder().orderId(orderId).eventType(type).build());
		event.setStatus(status);
		event.setUpdatedAt(Instant.now());
		orderEventRepository.save(event);
	}

	private void saveToOutbox(String aggregateType, String id, String type, Map<String, Object> payload) {
		outboxRepository.save(OutboxEvent.builder().aggregateType(aggregateType).aggregateId(id).eventType(type)
				.payload(payload).build());
	}

	/**
	 * Helper for methods that return a value (like createOrder)
	 */
	private <T> T withLock(String lockKey, int waitTime, int leaseTime, Supplier<T> action) {
		RLock lock = redissonClient.getLock(lockKey);
		boolean isLocked = false;
		try {
			isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
			if (!isLocked) {
				throw new RuntimeException("Operation in progress for key: " + lockKey + ". Please try again.");
			}
			return action.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Lock acquisition interrupted", e);
		} finally {
			if (isLocked) {
				lock.unlock();
			}
		}
	}

	/**
	 * Helper for void methods (like handleResponse)
	 */
	private void withLock(String lockKey, int waitTime, int leaseTime, Runnable action) {
		withLock(lockKey, waitTime, leaseTime, () -> {
			action.run();
			return null;
		});
	}
}