package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductViewDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

	@GetMapping("/ekart/v1/products/{skuId}")
	ProductViewDTO getProductBySku(@PathVariable("skuId") String skuId);
}