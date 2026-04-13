package com.ecommerce.productservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.entity.ProductView;

@Repository
public interface ProductViewRepository extends MongoRepository<ProductView, String>, ProductViewRepositoryCustom {

	Page<ProductView> findByNameContainingIgnoreCase(String name, Pageable pageable);

	@Query("{ 'variants.skuId': ?0 }")
	Optional<ProductView> findBySkuId(String skuId);

	@Aggregation(pipeline = { """
			{
			  $match: { 'status': 'ACTIVE' }
			}
			""", """
			{
			  $addFields: {
			    variants: {
			      $filter: {
			        input: '$variants',
			        as: 'v',
			        cond: { $eq: ['$$v.status', 'ACTIVE'] }
			      }
			    }
			  }
			}
			""" })
	List<ProductView> findAllActiveProductsAndVariants();
}
