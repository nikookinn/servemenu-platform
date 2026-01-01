package com.servemenu.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Global logging filter for all gateway requests
 * Logs:
 * - Request method, path, headers
 * - Response status, duration
 * - User information (if authenticated)
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();

        // Log request
        log.info("Gateway Request: {} {} from {}",
            request.getMethod(),
            request.getURI().getPath(),
            request.getRemoteAddress()
        );

        // Log request headers (excluding sensitive data)
        request.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Authorization") && 
                !name.equalsIgnoreCase("Cookie")) {
                log.debug("Request Header: {} = {}", name, values);
            }
        });

        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                ServerHttpResponse response = exchange.getResponse();
                Duration duration = Duration.between(startTime, Instant.now());
                
                log.info("Gateway Response: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode(),
                    duration.toMillis()
                );
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                
                log.error("Gateway Error: {} {} - Error: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    error.getMessage(),
                    duration.toMillis()
                );
            });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
