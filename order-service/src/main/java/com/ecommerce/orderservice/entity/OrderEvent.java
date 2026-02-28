package com.ecommerce.orderservice.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_events", uniqueConstraints = { @UniqueConstraint(columnNames = { "orderId", "eventType" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;

	@Column(nullable = false)
	private UUID orderId;

	@Column(nullable = false, length = 50)
	private String eventType;

	@Column(nullable = false, length = 50)
	private String status;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt = Instant.now();

	@UpdateTimestamp
	private Instant updatedAt = Instant.now();
}