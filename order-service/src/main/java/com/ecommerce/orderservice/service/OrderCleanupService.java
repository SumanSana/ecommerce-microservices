package com.ecommerce.orderservice.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupService {
	private final OrderService orderService;
	private final OrderRepository orderRepository;
	private final RedissonClient redissonClient; // Injected

	@Scheduled(fixedRate = 150000) // Runs every 2.5 minutes
	public void cancelAbandonedOrders() {
		RLock taskLock = redissonClient.getLock("lock:order-cleanup-task");

		try {
			// TryLock(0, ...): If it's already running on another server,
			// don't wait in line. Just skip this 150s cycle.
			if (taskLock.tryLock(0, 120, TimeUnit.SECONDS)) {
				try {
					log.info("Starting global abandoned order cleanup...");

					// Fix: Use Duration.ofMinutes
					Instant cutoff = Instant.now().minus(java.time.Duration.ofMinutes(15));
					List<Order> abandonedOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
							cutoff);

					log.info("Found {} abandoned orders to process.", abandonedOrders.size());

					for (Order order : abandonedOrders) {
						try {
							// We call the service so each cancellation gets its own lock/transaction
							orderService.cancelOrder(order.getId());
						} catch (Exception e) {
							log.error("Failed to cancel order {}: {}", order.getId(), e.getMessage());
						}
					}
				} finally {
					if (taskLock.isHeldByCurrentThread()) {
						taskLock.unlock();
					}
				}
			} else {
				log.debug("Cleanup task already running on another instance. Skipping this cycle.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Cleanup task interrupted", e);
		}
	}
}