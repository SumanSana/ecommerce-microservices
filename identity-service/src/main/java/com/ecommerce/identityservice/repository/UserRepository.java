package com.ecommerce.identityservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.identityservice.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByMobileNumber(String mobileNumber);

	Optional<User> findByEmail(String email);

	boolean existsByMobileNumber(String mobileNumber);

	boolean existsByEmail(String email);
}
