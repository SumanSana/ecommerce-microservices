package com.ecommerce.apigateway;

import java.util.Arrays;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

@Configuration
public class ApiGatewayConfig {

	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration corsConfig = new CorsConfiguration();

		// Allow your Angular dev server
		corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

		// Allow all standard methods
		corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

		// Allow all headers (important for Authorization Bearer tokens)
		corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

		// Allow credentials if you use cookies later
		corsConfig.setAllowCredentials(true);

		// How long the browser should cache this CORS response
		corsConfig.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsWebFilter(source);
	}
}