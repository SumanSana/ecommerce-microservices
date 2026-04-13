package com.ecommerce.productservice.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.productservice.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
	Optional<ProductVariant> findBySkuId(String skuId);

	@Query("SELECT pv.price FROM ProductVariant pv WHERE pv.skuId = :skuId")
	BigDecimal findPriceBySkuId(String skuId);
}