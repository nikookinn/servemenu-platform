package com.servemenu.businessservice.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JwtUtil {

    /**
     * Extract user ID from JWT token
     */
    public UUID extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String userIdStr = jwt.getClaim("userId");
            if (userIdStr != null) {
                return UUID.fromString(userIdStr);
            }
        }
        throw new IllegalStateException("Unable to extract userId from authentication");
    }

    /**
     * Extract Keycloak user ID from JWT token
     */
    public UUID extractKeycloakUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String subject = jwt.getSubject();
            if (subject != null) {
                return UUID.fromString(subject);
            }
        }
        throw new IllegalStateException("Unable to extract keycloakUserId from authentication");
    }

    /**
     * Extract email from JWT token
     */
    public String extractEmail(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim("email");
        }
        return null;
    }

    /**
     * Check if user has role
     */
    public boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
