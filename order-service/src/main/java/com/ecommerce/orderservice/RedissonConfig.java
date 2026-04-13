package com.ecommerce.orderservice;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port}")
	private String redisPort;

	@Value("${spring.data.redis.password}")
	private String redisPassword;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();

		// Use single server configuration
		String address = String.format("redis://%s:%s", redisHost, redisPort);
		var serverConfig = config.useSingleServer().setAddress(address).setConnectionMinimumIdleSize(5)
				.setConnectionPoolSize(20).setRetryAttempts(3).setRetryInterval(1500);

		if (redisPassword != null && !redisPassword.isBlank()) {
			serverConfig.setPassword(redisPassword);
		}

		return Redisson.create(config);
	}
}