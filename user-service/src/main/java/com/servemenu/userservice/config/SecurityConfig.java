package com.servemenu.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS handled by API Gateway - no need to configure here
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Public endpoints
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()

                        // Customer registration - public
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers/register").permitAll()

                        // All other API endpoints require authentication
                        .requestMatchers("/api/v1/**").authenticated()

                        // Actuator management endpoints
                        .requestMatchers("/actuator/**").hasRole("MONITORING")

                        // Deny all other requests
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);

            // Extract Keycloak realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<GrantedAuthority> realmRoles = extractRealmRoles(realmAccess);

            // Extract Keycloak resource roles (client-specific)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            Collection<GrantedAuthority> resourceRoles = extractResourceRoles(resourceAccess);

            // Combine all authorities
            return Stream.of(authorities, realmRoles, resourceRoles)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        });

        return converter;
    }

    // ============ Private Helper Methods ============

    private Collection<GrantedAuthority> extractRealmRoles(Map<String, Object> realmAccess) {
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof List<?>)) {
            return List.of();
        }

        return ((List<?>) roles).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    private Collection<GrantedAuthority> extractResourceRoles(Map<String, Object> resourceAccess) {
        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return List.of();
        }

        return resourceAccess.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map)
                .flatMap(entry -> {
                    Map<?, ?> resource = (Map<?, ?>) entry.getValue();
                    Object roles = resource.get("roles");

                    if (!(roles instanceof List<?>)) {
                        return Stream.empty();
                    }

                    return ((List<?>) roles).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority(
                                    "ROLE_" + entry.getKey().toString().toUpperCase() + "_" + role.toUpperCase()
                            ));
                })
                .collect(Collectors.toList());
    }
}