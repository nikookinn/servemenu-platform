package com.servemenu.mediaservice.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration with Retry Mechanism
 * Implements exponential backoff retry strategy for failed message processing
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    /**
     * Kafka Listener Container Factory with Manual Acknowledgment and Error Handling
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        
        // Manual acknowledgment for better control
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // Custom error handler with retry
        factory.setCommonErrorHandler(customErrorHandler());
        
        // Concurrency for parallel processing
        factory.setConcurrency(3);

        return factory;
    }

    /**
     * Avro Kafka Listener Container Factory for consuming Avro messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> avroKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(avroConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(customErrorHandler());
        factory.setConcurrency(3);

        return factory;
    }

    /**
     * Consumer Factory for Avro messages with Schema Registry
     */
    @Bean
    public ConsumerFactory<String, Object> avroConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "media-service-qr-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Custom Error Handler with Exponential Backoff Retry
     * Retry Strategy:
     * - Initial interval: 1 second
     * - Multiplier: 2.0 (exponential backoff)
     * - Max interval: 10 seconds
     * - Max attempts: 3
     */
    @Bean
    public CommonErrorHandler customErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);      // 1 second
        backOff.setMultiplier(2.0);             // 2x each retry
        backOff.setMaxInterval(10000L);         // Max 10 seconds
        backOff.setMaxElapsedTime(30000L);      // Total max 30 seconds

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            // This is called after all retries are exhausted
            log.error("💀 DEAD LETTER: Failed to process message after retries: topic={}, partition={}, offset={}", 
                    ((ConsumerRecord<?, ?>) record).topic(),
                    ((ConsumerRecord<?, ?>) record).partition(),
                    ((ConsumerRecord<?, ?>) record).offset(),
                    exception);
            
            // TODO: Send to Dead Letter Queue (DLQ) topic for manual investigation
            // Example: kafkaTemplate.send("media.dlq", record.key(), record.value());
            
        }, backOff);

        // Don't retry for these exceptions (they won't succeed on retry)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                com.fasterxml.jackson.core.JsonProcessingException.class
        );

        return errorHandler;
    }

    /**
     * Avro Serializer Bean for Outbox Event Publishing
     * Automatically configured with Schema Registry URL from Config Service
     */
    @Bean
    public KafkaAvroSerializer kafkaAvroSerializer() {
        KafkaAvroSerializer serializer = new KafkaAvroSerializer();
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", schemaRegistryUrl);
        serializer.configure(config, false);
        return serializer;
    }
}
