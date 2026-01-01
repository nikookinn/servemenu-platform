package com.servemenu.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enable Spring Retry for Keycloak operations
 * Used by @Retryable annotations in KeycloakUserManagementService
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry is enabled for methods annotated with @Retryable
}