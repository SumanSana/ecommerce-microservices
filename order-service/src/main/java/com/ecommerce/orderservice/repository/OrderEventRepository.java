package com.ecommerce.orderservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.orderservice.entity.OrderEvent;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

	List<OrderEvent> findByOrderId(UUID orderId);

	Optional<OrderEvent> findByOrderIdAndEventType(UUID orderId, String eventType);
}
