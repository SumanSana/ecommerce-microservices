package com.ecommerce.inventoryservice.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_id")
	private Inventory inventory;

	@Column(name = "sku_id", nullable = false)
	private String skuId;

	@Column(name = "transaction_type", nullable = false)
	private String transactionType;
	
	@Column(name = "reference_type", nullable = false)
	private String referenceType;
	
	@Column(name = "reference_id", nullable = false)
	private String referenceId;

	@Column(name = "quantity_changed", nullable = false)
	private Integer quantityChanged;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;
}