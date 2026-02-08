package com.ecommerce.productservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.productservice.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {}