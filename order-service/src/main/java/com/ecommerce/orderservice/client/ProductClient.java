package com.ecommerce.orderservice.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

	@GetMapping("/ekart/v1/products/variants/{skuId}/price")
	BigDecimal getSkuPrice(@PathVariable("skuId") String skuId);
}