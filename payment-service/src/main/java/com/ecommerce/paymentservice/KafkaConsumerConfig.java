package com.ecommerce.paymentservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ecommerce.paymentservice.dto.RefundEvent;

@Configuration
public class KafkaConsumerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	@Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
	private String autoOffsetReset;

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, RefundEvent> refund() {
		return createFactory(RefundEvent.class);
	}

	private <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(Class<T> targetClass) {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

		// THIS IS THE MISSING PIECE:
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

		// Use 'false' to ignore type headers from the producer and use our local DTO
		JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<>(targetClass, false);

		DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(props,
				new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));

		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
}
