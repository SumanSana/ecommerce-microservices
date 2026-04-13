package com.ecommerce.productservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.productservice.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
	@Query("SELECT c FROM Category c WHERE c.subCategories IS EMPTY")
	List<Category> findAllLeafCategories();
}