package com.ecommerce.productservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.productservice.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, UUID> {}