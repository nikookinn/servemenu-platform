package com.servemenu.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Debug filter to log JWT token information
 */
@Component
public class JwtDebugFilter implements WebFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtDebugFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Log authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader != null) {
            log.debug("=== JWT Debug Info ===");
            log.debug("Path: {}", path);
            log.debug("Authorization Header Present: YES");
            log.debug("Header starts with 'Bearer ': {}", authHeader.startsWith("Bearer "));
            
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.debug("Token length: {}", token.length());
                log.debug("Token preview: {}...", token.substring(0, Math.min(50, token.length())));
            } else {
                log.warn("Authorization header does not start with 'Bearer '");
            }
        } else {
            log.debug("=== JWT Debug Info ===");
            log.debug("Path: {}", path);
            log.debug("Authorization Header: MISSING");
        }
        
        return chain.filter(exchange);
    }
}
