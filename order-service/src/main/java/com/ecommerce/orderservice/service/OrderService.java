package com.ecommerce.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.Address;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.dto.ProductViewDTO;
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

	@Transactional
	public OrderResponse createOrder(OrderRequest request) {
		log.info("Creating order for customer: {}", request.customerId());

		List<OrderItem> orderItems = request.items().stream().map(itemReq -> {
			ProductViewDTO product = productClient.getProductBySku(itemReq.skuId());
			return OrderItem.builder().skuId(product.skuId()).quantity(itemReq.quantity()).price(product.price())
					.build();
		}).toList();

		BigDecimal totalAmount = orderItems.stream()
				.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Order order = Order.builder().customerId(request.customerId()).customerEmail(request.customerEmail())
				.status(OrderStatus.PENDING).totalAmount(totalAmount).shippingAddressLine1(request.addressLine1())
				.shippingCity(request.city()).shippingZipCode(request.zipCode()).shippingCountry(request.country())
				.items(orderItems).build();

		Order savedOrder = orderRepository.save(order);

		PaymentRequest paymentRequest = new PaymentRequest(savedOrder.getId(), savedOrder.getCustomerEmail() ,savedOrder.getTotalAmount());
		Map<String, String> paymentResponse = paymentClient.initiate(paymentRequest);

		saveEventLog(savedOrder.getId(), "ORDER", "PENDING");

		saveToOutbox("ORDER", savedOrder.getId().toString(), "PENDING",
				Map.of("orderId", order.getId(), "customerId", order.getCustomerId(), "customerEmail", order.getCustomerEmail(), "totalAmount",
						order.getTotalAmount(), "status", order.getStatus(), "items", order.getItems()));

		return new OrderResponse(savedOrder.getId(), "PENDING", totalAmount, paymentResponse.get("clientSecret"));
	}

	@Transactional
	public void handleInventoryResponse(UUID orderId, boolean success) {
		saveEventLog(orderId, "INVENTORY", success ? "SUCCESS" : "FAILURE");
		verifyAndProcessOrder(orderId);
	}

	@Transactional
	public void handlePaymentResponse(UUID orderId, String status) {
		boolean success = "SUCCESS".equalsIgnoreCase(status);
		saveEventLog(orderId, "PAYMENT", success ? "SUCCESS" : "FAILURE");
		verifyAndProcessOrder(orderId);
	}

	@Transactional
	public void cancelOrder(UUID id) {
		Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order id not found: " + id));
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		saveEventLog(order.getId(), "ORDER", "CANCELLED");
		saveToOutbox("ORDER", order.getId().toString(), "CANCELLED",
				Map.of("orderId", order.getId(), "customerEmail", order.getCustomerEmail(), "totalAmount",
						order.getTotalAmount(), "status", order.getStatus(), "items", order.getItems()));
		saveToOutbox("PAYMENT", order.getId().toString(), "REFUND",
				Map.of("orderId", order.getId(), "customerEmail", order.getCustomerEmail(), "amount", order.getTotalAmount()));

	}

	@Transactional
	public void verifyAndProcessOrder(UUID orderId) {
		List<OrderEvent> events = orderEventRepository.findByOrderId(orderId);
		Order order = orderRepository.findById(orderId).orElseThrow();

		if (order.getStatus() != OrderStatus.PENDING)
			return;

		Optional<OrderEvent> invEvent = events.stream().filter(e -> "INVENTORY".equals(e.getEventType()))
				.findFirst();
		Optional<OrderEvent> payEvent = events.stream().filter(e -> "PAYMENT".equals(e.getEventType()))
				.findFirst();

		// Check for Failures
		boolean isInvFailed = invEvent.isPresent() && "FAILURE".equals(invEvent.get().getStatus());
		boolean isPayFailed = payEvent.isPresent() && "FAILURE".equals(payEvent.get().getStatus());

		if (isInvFailed || isPayFailed) {
			cancelOrder(order.getId());
			return;
		}

		// Check for Successes
		boolean isInvSuccess = invEvent.isPresent() && "SUCCESS".equals(invEvent.get().getStatus());
		boolean isPaySuccess = payEvent.isPresent() && "SUCCESS".equals(payEvent.get().getStatus());

		if (isInvSuccess && isPaySuccess) {
			order.setStatus(OrderStatus.CONFIRMED);
			orderRepository.save(order);
			saveEventLog(order.getId(), "ORDER", "CONFIRMED");
			saveToOutbox("ORDER", order.getId().toString(), "CONFIRMED",
					Map.of("orderId", order.getId(), "customerId", order.getCustomerId(), "customerEmail", order.getCustomerEmail(), "totalAmount",
							order.getTotalAmount(), "status", order.getStatus(), "items", order.getItems()));
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

	@Transactional
	public void updateShippingAddress(UUID orderId, Address newAddress) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order " + orderId + " not found"));

		// Validation: Only allow update if order is in a changeable state
		if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED
				|| order.getStatus() == OrderStatus.CANCELLED) {
			throw new IllegalStateException("Address cannot be updated for order in status: " + order.getStatus());
		}

		// Direct mapping
		order.setShippingAddressLine1(newAddress.shippingAddressLine1());
		order.setShippingCity(newAddress.shippingCity());
		order.setShippingZipCode(newAddress.shippingZipCode());
		order.setShippingCountry(newAddress.shippingCountry());
		orderRepository.save(order);
		saveEventLog(order.getId(), "SHIPPING-ADDRESS", "UPDATED");
		log.info("Address updated for Order: {}", orderId);
	}
}