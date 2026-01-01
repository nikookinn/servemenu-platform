package com.servemenu.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security Configuration for API Gateway
 * 
 * Features:
 * - JWT-based authentication with Keycloak
 * - Role-based access control
 * - CORS configuration
 * - Public endpoints (health checks, actuator)
 * - Reactive security (WebFlux)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Main security filter chain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable session management - use stateless JWT
            .securityContextRepository(org.springframework.security.web.server.context.NoOpServerSecurityContextRepository.getInstance())
            // Add logging
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.authenticationEntryPoint((exchange, ex) -> {
                    log.error("Authentication failed: {}", ex.getMessage());
                    return Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED));
                })
            )
            .authorizeExchange(exchanges -> exchanges
                // CORS preflight requests
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints - No authentication required
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/actuator/prometheus").permitAll()
                .pathMatchers("/fallback/**").permitAll()
                
                // User Service - Public endpoints
                .pathMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/customers").permitAll()
                
                // User Service - Authenticated endpoints
                .pathMatchers("/api/v1/users/me").authenticated()
                .pathMatchers("/api/v1/users/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // Business Owner endpoints
                .pathMatchers("/api/v1/business-owners/**").hasRole("business_owner")
                
                // Store User endpoints
                .pathMatchers("/api/v1/store-users/**").hasAnyRole("business_owner", "store_admin")
                
                // Customer endpoints
                .pathMatchers("/api/v1/customers/me").hasRole("customer")
                .pathMatchers("/api/v1/customers/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // Business Service - Business endpoints
                .pathMatchers(HttpMethod.POST, "/api/v1/businesses/setup").hasRole("business_owner")
                .pathMatchers("/api/v1/businesses/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // Business Service - Store endpoints
                .pathMatchers("/api/v1/stores/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // Business Service - Menu endpoints
                .pathMatchers("/api/v1/menus/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // Business Service - Table endpoints
                .pathMatchers("/api/v1/tables/**").hasAnyRole("business_owner", "store_admin", "store_user")
                
                // All other requests require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
            );

        log.info("Security filter chain configured with stateless JWT authentication");
        return http.build();
    }

    /**
     * Extract roles from Keycloak JWT token
     * Converts realm_access.roles to Spring Security GrantedAuthority
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    /**
     * CORS Configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Allow all origins for development
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configuration initialized - allowing all origins for development");
        return source;
    }

    /**
     * Custom converter to extract Keycloak roles
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        private static final Logger log = LoggerFactory.getLogger(KeycloakRoleConverter.class);
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            log.debug("=== Converting JWT to Authorities ===");
            log.debug("JWT Subject: {}", jwt.getSubject());
            log.debug("JWT Claims: {}", jwt.getClaims().keySet());
            
            // Extract realm_access.roles from Keycloak JWT
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                log.warn("No realm_access or roles found in JWT token");
                log.debug("Available claims: {}", jwt.getClaims());
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            
            log.debug("Extracted roles from token: {}", roles);

            Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            log.debug("Granted authorities: {}", authorities);
            
            return authorities;
        }
    }
}
