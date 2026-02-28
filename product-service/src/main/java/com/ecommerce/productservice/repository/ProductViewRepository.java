package com.ecommerce.productservice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.entity.ProductView;

@Repository
public interface ProductViewRepository extends MongoRepository<ProductView, String>, ProductViewRepositoryCustom {

	Page<ProductView> findByNameContainingIgnoreCase(String name, Pageable pageable);
	Optional<ProductView> findBySkuId(String skuId);
}
