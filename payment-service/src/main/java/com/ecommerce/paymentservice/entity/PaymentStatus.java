package com.ecommerce.paymentservice.entity;

public enum PaymentStatus {
    INITIATED,      // Intent created
    SUCCESS,        // Payment cleared
    FAILED,         // Card declined
    REFUNDED,       // Refund processed successfully
    REFUND_FAILED,  // Stripe blocked the refund
    CANCELLED       // Order cancelled before payment was made
}