package com.ecommerce.productservice.repository;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ecommerce.productservice.entity.ProductView;

public interface ProductViewRepositoryCustom {
    Page<ProductView> findByFilters(String brand, String category, 
                                   BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}