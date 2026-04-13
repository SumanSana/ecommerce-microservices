package com.ecommerce.productservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecommerce.productservice.dto.ProductSearchCriteria;
import com.ecommerce.productservice.entity.ProductView;

public interface ProductViewRepositoryCustom {
	void updateInventory(String skuId, Integer newQuantity);
	Page<ProductView> findByFilters(ProductSearchCriteria criteria, Pageable pageable);
}