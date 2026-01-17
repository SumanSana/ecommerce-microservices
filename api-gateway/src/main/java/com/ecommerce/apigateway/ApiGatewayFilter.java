package com.ecommerce.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.ecommerce.apigateway.util.JwtUtil;

import reactor.core.publisher.Mono;

@Component
public class ApiGatewayFilter implements GlobalFilter, Ordered {

	private final JwtUtil jwtUtil;

	public ApiGatewayFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		
		String path = exchange.getRequest().getURI().getPath();

		if (path.contains("/auth/token") || path.contains("/auth/register")) {
			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return unAuthorized(exchange);
		}

		String token = authHeader.substring(7);

		try {
			
			jwtUtil.validateAndGetClaims(token);

		} catch (Exception ex) {
			return unAuthorized(exchange);
		}

		return chain.filter(exchange);
	}

	private Mono<Void> unAuthorized(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}

	@Override
	public int getOrder() {
		return -1; 
	}
}