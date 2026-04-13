package com.ecommerce.inventoryservice.exception;

//Make sure it is this exact import
public class SkuNotFoundException extends RuntimeException {
	public SkuNotFoundException(String skuId) {
		super("SKU not found: " + skuId);
	}
}