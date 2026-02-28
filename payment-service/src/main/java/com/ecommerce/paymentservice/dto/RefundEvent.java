package com.ecommerce.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundEvent(UUID orderId, String customerEmail, BigDecimal amount) {

}
