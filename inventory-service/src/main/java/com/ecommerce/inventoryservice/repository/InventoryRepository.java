package com.ecommerce.inventoryservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.inventoryservice.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, UUID>{
	
	public boolean existsBySkuId(String skuId);

	public Optional<Inventory> findBySkuId(String skuId);
	
	@Modifying
    @Query("""
        UPDATE Inventory i 
        SET i.reservedQuantity = i.reservedQuantity + :quantity, 
            i.updatedAt = CURRENT_TIMESTAMP 
        WHERE i.skuId = :skuId 
        AND (i.totalQuantity - i.reservedQuantity) >= :quantity
    """)
    public int reserveStockAtomic(String skuId, int quantity);
	
	// Path A: Commit (Sold)
	@Modifying
	@Query("""
	    UPDATE Inventory i 
	    SET i.totalQuantity = i.totalQuantity - :qty, 
	        i.reservedQuantity = i.reservedQuantity - :qty 
	    WHERE i.skuId = :skuId AND i.reservedQuantity >= :qty
	""")
	int commitStock(String skuId, int qty);

	// Path B: Release (Cancelled)
	@Modifying
	@Query("""
	    UPDATE Inventory i 
	    SET i.reservedQuantity = i.reservedQuantity - :qty 
	    WHERE i.skuId = :skuId AND i.reservedQuantity >= :qty
	""")
	int releaseStock(String skuId, int qty);
	
	@Modifying
	@Query("""
	    UPDATE Inventory i 
	    SET i.totalQuantity = i.totalQuantity + :qty 
	    WHERE i.skuId = :skuId
	""")
	int rollbackInventoryForCancelledOrder(String skuId, int qty);

	public List<Inventory> findBySkuIdContainingIgnoreCase(String term, Pageable limit);
}
