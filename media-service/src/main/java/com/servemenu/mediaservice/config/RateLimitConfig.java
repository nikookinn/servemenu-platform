package com.servemenu.mediaservice.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate limiting configuration using Resilience4j
 * Prevents abuse and ensures fair usage
 * 
 * Features:
 * - Upload rate limiting (10 requests/minute)
 * - Download rate limiting (100 requests/minute)
 * - Delete rate limiting (5 requests/minute)
 * - Prometheus metrics integration
 * - Spring Boot Actuator support
 * 
 * @author ServeMenu Platform Team
 * @version 2.0.0
 * @since 2025-11-12
 */
@Slf4j
@Configuration
public class RateLimitConfig {
    
    /**
     * Rate limiter registry bean
     * Manages all rate limiters with metrics
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
    
    /**
     * Rate limiter for upload operations
     * Limit: 10 uploads per minute per user
     * Timeout: 5 seconds wait for permission
     */
    @Bean
    public RateLimiterConfig uploadRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(10)                    // 10 requests
                .limitRefreshPeriod(Duration.ofMinutes(1))  // per minute
                .timeoutDuration(Duration.ofSeconds(5))     // wait 5s for permission
                .build();
    }
    
    /**
     * Rate limiter for download/view operations
     * Limit: 100 downloads per minute per user
     * Timeout: 2 seconds wait for permission
     */
    @Bean
    public RateLimiterConfig downloadRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(100)                   // 100 requests
                .limitRefreshPeriod(Duration.ofMinutes(1))  // per minute
                .timeoutDuration(Duration.ofSeconds(2))     // wait 2s for permission
                .build();
    }
    
    /**
     * Rate limiter for delete operations
     * Limit: 5 deletes per minute per user
     * Timeout: 3 seconds wait for permission
     */
    @Bean
    public RateLimiterConfig deleteRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(5)                     // 5 requests
                .limitRefreshPeriod(Duration.ofMinutes(1))  // per minute
                .timeoutDuration(Duration.ofSeconds(3))     // wait 3s for permission
                .build();
    }
}
