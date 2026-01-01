package com.servemenu.userservice.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotBlank;

@Configuration
@Validated
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfig.class);

    @NotBlank(message = "Keycloak realm must be configured")
    private String realm;

    @NotBlank(message = "Keycloak auth server URL must be configured")
    private String authServerUrl;

    @NotBlank(message = "Keycloak admin client ID must be configured")
    private String adminClientId;

    @NotBlank(message = "Keycloak admin client secret must be configured")
    private String adminClientSecret;

    private String adminRealm = "master";

    @Bean
    public Keycloak keycloakAdminClient() {
        logger.info("Initializing Keycloak Admin Client");
        logger.info("Server URL: {}, Realm: {}", authServerUrl, realm);

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .build();

        try {
            keycloak.tokenManager().getAccessToken();
            logger.info("Successfully connected to Keycloak");
        } catch (Exception e) {
            logger.error("Failed to connect to Keycloak: {}", e.getMessage());
            throw new IllegalStateException("Cannot connect to Keycloak", e);
        }

        return keycloak;
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up Keycloak resources");
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public String getAdminRealm() {
        return adminRealm;
    }

    public void setAdminRealm(String adminRealm) {
        this.adminRealm = adminRealm;
    }
}