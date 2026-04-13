package com.ecommerce.identityservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.identityservice.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByMobile(String mobil);

	Optional<User> findByEmail(String email);

	boolean existsByMobile(String mobile);

	boolean existsByEmail(String email);
}
