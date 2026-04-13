package com.ecommerce.paymentservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.paymentservice.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    // Crucial for idempotency
    Optional<Payment> findByOrderId(UUID orderId);
    
    // Crucial for Webhook lookup
    Optional<Payment> findByStripePaymentIntentId(String stripeId);
}