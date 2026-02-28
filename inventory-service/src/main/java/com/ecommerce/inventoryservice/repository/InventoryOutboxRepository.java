package com.ecommerce.inventoryservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.inventoryservice.entity.InventoryOutbox;

public interface InventoryOutboxRepository extends JpaRepository<InventoryOutbox, UUID>{
	public List<InventoryOutbox> findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();
}
