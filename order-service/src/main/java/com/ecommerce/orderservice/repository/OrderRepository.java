package com.ecommerce.orderservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

	List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant cutoff);

	List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}