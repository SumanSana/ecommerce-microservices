package com.ecommerce.orderservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCleanupService {
	private final OrderService orderService;
	private final OrderRepository orderRepository;

	@Scheduled(fixedRate = 150000)
	@Transactional
	public void cancelAbandonedOrders() {
		Instant cutoff = Instant.now().minus(Duration.ofMinutes(15));
		List<Order> abandonedOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);
		for (Order order : abandonedOrders) {
			orderService.cancelOrder(order.getId());
		}
	}
}