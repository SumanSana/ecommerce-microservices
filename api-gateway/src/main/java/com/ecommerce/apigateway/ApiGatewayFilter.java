package com.ecommerce.apigateway;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.ecommerce.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class ApiGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh"
    );

    public ApiGatewayFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Check for public endpoints
        boolean isPathSecured = openApiEndpoints.stream().noneMatch(path::contains);
        if (!isPathSecured) {
            return chain.filter(exchange);
        }

        // 2. Extract Authorization Header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unAuthorized(exchange);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validate Token and Get Claims
            Claims claims = jwtUtil.validateAndGetClaims(token);

            // 4. Role-Based Access Control (RBAC) Enhancement
            // Check if the path is an admin path
            if (path.contains("/admin")) {
                String role = claims.get("role", String.class);
                
                if (role == null || !role.contains("ADMIN")) {
                    return forbidden(exchange); // Valid token, but insufficient permissions
                }
            }

        } catch (Exception ex) {
            return unAuthorized(exchange);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unAuthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // New helper for Access Denied (Role Mismatch)
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}