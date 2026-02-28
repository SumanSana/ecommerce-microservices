package com.ecommerce.orderservice.entity;

public enum OrderStatus {
    PENDING,            // Initial state
    AWAITING_PAYMENT,   // Inventory reserved, waiting for Stripe
    CONFIRMED,          // Paid and ready to ship
    CANCELLED,          // Failed inventory or failed payment
    SHIPPED,            // In transit
    DELIVERED,          // Arrived at customer
    RETURNED
}