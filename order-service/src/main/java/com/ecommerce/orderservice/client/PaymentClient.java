package com.ecommerce.orderservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.ecommerce.orderservice.dto.PaymentRequest;

@FeignClient(name = "payment-service")
public interface PaymentClient {
	@PostMapping("/ekart/v1/payments/initiate")
	public Map<String, String> initiate(PaymentRequest request);
}
