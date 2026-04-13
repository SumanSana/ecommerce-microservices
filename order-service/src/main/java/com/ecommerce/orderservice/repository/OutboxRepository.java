package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetches the oldest unprocessed events. 
     * We use 'findTop20' to avoid loading thousands of records into memory at once 
     * if the message broker (Kafka) goes down.
     */
    List<OutboxEvent> findTop20ByProcessedAtIsNullOrderByCreatedAtAsc();
}