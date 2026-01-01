package com.servemenu.mediaservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration for Media Service
 * 
 * Strategic Caching:
 * - Media metadata (frequently queried, rarely changed)
 * - Entity media lists (frequently queried)
 * - Storage statistics (expensive calculation)
 * - Existence checks (very frequent)
 * 
 * NOT Cached:
 * - Upload operations (always unique)
 * - Pre-signed URLs (expire in 15 min)
 * - Image processing (always unique)
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {
    
    // Cache names
    public static final String MEDIA_BY_ID = "media:byId";
    public static final String MEDIA_BY_ENTITY = "media:byEntity";
    public static final String MEDIA_EXISTS = "media:exists";
    public static final String STORAGE_STATS = "media:stats";
    
    /**
     * Configure cache manager with different TTLs for different caches
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis Cache Manager for Media Service");
        
        // Create ObjectMapper with type information for polymorphic deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(30)); // Default TTL: 30 minutes
        
        // Custom TTL for specific caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Media by ID: 1 hour (frequently accessed, rarely changes)
        cacheConfigurations.put(MEDIA_BY_ID, defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Media by Entity: 30 minutes (moderately accessed)
        cacheConfigurations.put(MEDIA_BY_ENTITY, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Media exists: 15 minutes (very frequent, lightweight)
        cacheConfigurations.put(MEDIA_EXISTS, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Storage stats: 5 minutes (expensive calculation, can be slightly stale)
        cacheConfigurations.put(STORAGE_STATS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        log.info("Redis cache configurations:");
        log.info("  - {}: TTL 1 hour", MEDIA_BY_ID);
        log.info("  - {}: TTL 30 minutes", MEDIA_BY_ENTITY);
        log.info("  - {}: TTL 15 minutes", MEDIA_EXISTS);
        log.info("  - {}: TTL 5 minutes", STORAGE_STATS);
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}
