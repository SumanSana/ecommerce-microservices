package com.ecommerce.identityservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.identityservice.entity.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
	Optional<Role> findByName(String name);
}
