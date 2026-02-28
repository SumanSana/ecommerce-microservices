package com.ecommerce.productservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.entity.OutboxEvent;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

	List<OutboxEvent> findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();
}