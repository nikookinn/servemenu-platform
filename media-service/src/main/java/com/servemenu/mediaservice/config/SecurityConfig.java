package com.servemenu.mediaservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security Configuration for Media Service
 * 
 * Role-based access control:
 * - business_owner: Full access (upload, delete, replace, view, stats)
 * - store_admin: Management access (upload, delete, replace, view)
 * - store_user: Read-only access (view only)
 * - customer: Read-only access (view only - for menu item images)
 * 
 * Public endpoints:
 * - Health checks
 * - Actuator endpoints (for monitoring)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    
    /**
     * Main security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // Enable CORS for direct frontend access (file uploads bypass API Gateway)
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints - No authentication
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/api/v1/media/health").permitAll()
                .requestMatchers("/api/v1/media/cache/clear").permitAll() // Debug endpoint
                
                // Media Upload/Modification - business_owner, store_admin ONLY
                .requestMatchers(HttpMethod.POST, "/api/v1/media/upload")
                    .hasAnyRole("business_owner", "store_admin")
                .requestMatchers(HttpMethod.PUT, "/api/v1/media/**")
                    .hasAnyRole("business_owner", "store_admin")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/media/**")
                    .hasAnyRole("business_owner", "store_admin")
                
                // Media Stats - business_owner only
                .requestMatchers("/api/v1/media/entity/*/stats")
                    .hasRole("business_owner")
                
                // Media View (GET, HEAD) - All authenticated users
                // Customers need to see menu item images
                .requestMatchers(HttpMethod.GET, "/api/v1/media/**").authenticated()
                .requestMatchers(HttpMethod.HEAD, "/api/v1/media/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        log.info("Media Service Security configured with JWT authentication");
        log.info("Roles: business_owner (full), store_admin (manage), store_user (view), customer (view)");
        
        return http.build();
    }
    
    /**
     * JWT Authentication Converter
     * Extracts roles from Keycloak JWT token
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
    
    /**
     * CORS Configuration
     * Allow direct frontend access for file uploads
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configured for direct frontend access");
        
        return source;
    }
    
    /**
     * Custom converter to extract Keycloak roles from JWT
     * Converts realm_access.roles to Spring Security GrantedAuthority
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        private static final Logger log = LoggerFactory.getLogger(KeycloakRoleConverter.class);
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            log.debug("=== Converting JWT to Authorities (Media Service) ===");
            log.debug("JWT Subject: {}", jwt.getSubject());
            
            // Extract realm_access.roles from Keycloak JWT
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                log.warn("No realm_access or roles found in JWT token");
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
