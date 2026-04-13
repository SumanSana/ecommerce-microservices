package com.ecommerce.inventoryservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.inventoryservice.entity.InventoryTransaction;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID>{
	public boolean existsByReferenceIdAndTransactionType(String referenceId, String transactionType);
	public boolean existsByReferenceIdAndReferenceType(String referenceId, String referenceType);
	public List<InventoryTransaction> findByReferenceIdAndReferenceType(String string, String string2);
}
