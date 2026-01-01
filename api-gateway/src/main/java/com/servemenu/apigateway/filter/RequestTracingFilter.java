package com.servemenu.apigateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Request tracing filter
 * 
 * Adds correlation ID to all requests for distributed tracing
 * - Generates unique correlation ID if not present
 * - Propagates correlation ID to downstream services
 * - Adds correlation ID to response headers
 */
@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Get or generate correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        // Generate request ID
        String requestId = UUID.randomUUID().toString();

        // Add to MDC for logging
        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);

        // Add headers to request
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, correlationId)
            .header(REQUEST_ID_HEADER, requestId)
            .build();

        // Add headers to response
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .doFinally(signalType -> {
                // Clean up MDC
                MDC.remove("correlationId");
                MDC.remove("requestId");
            });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
