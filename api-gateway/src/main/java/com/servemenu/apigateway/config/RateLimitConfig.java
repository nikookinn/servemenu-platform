package com.servemenu.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Configuration
 * Uses Redis-based rate limiting with user-specific keys
 * - Authenticated users: Limited by user ID
 * - Anonymous users: Limited by IP address
 */
@Configuration
public class RateLimitConfig {

    /**
     * User-based rate limiting key resolver (PRIMARY)
     * For authenticated users: Uses user ID from JWT
     * For anonymous users: Uses IP address
     */
    @Bean
    @Primary  // Default key resolver for rate limiting
    public KeyResolver userKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .switchIfEmpty(Mono.just(
                exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress()
            ));
    }

    /**
     * IP-based rate limiting key resolver
     * Uses client IP address for rate limiting
     * Useful for public endpoints
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress()
        );
    }

    /**
     * API Key-based rate limiting (for future use)
     * Uses X-API-Key header for rate limiting
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-API-Key");
            
            return Mono.justOrEmpty(apiKey)
                .switchIfEmpty(Mono.just("anonymous"));
        };
    }
}
