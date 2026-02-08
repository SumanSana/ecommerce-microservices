package com.ecommerce.productservice.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.entity.ProductView;

@Repository
public interface ProductViewRepository extends MongoRepository<ProductView, String> {

	Page<ProductView> findByNameContainingIgnoreCase(String name, Pageable pageable);

	// Using a dynamic query: if a parameter is null, the query ignores it
	@Query("{ $and: [ " + "{ ?0 = null OR brandName: ?0 }, " + "{ ?1 = null OR categoryName: ?1 }, "
			+ "{ ?2 = null OR price: { $gte: ?2 } }, " + "{ ?3 = null OR price: { $lte: ?3 } } " + "] }")
	Page<ProductView> findByFilters(String brand, String category, BigDecimal minPrice, BigDecimal maxPrice,
			Pageable pageable);
}
