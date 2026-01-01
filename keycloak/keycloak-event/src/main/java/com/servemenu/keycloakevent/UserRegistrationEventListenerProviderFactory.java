package com.servemenu.keycloakevent;

import com.servemenu.keycloakevent.publisher.KafkaEventPublisher;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserRegistrationEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(UserRegistrationEventListenerProviderFactory.class);

    public static final String ID = "servemenu-kafka-publisher";

    private static final String KAFKA_BOOTSTRAP_SERVERS_ENV = "KC_KAFKA_BOOTSTRAP_SERVERS";

    private KafkaEventPublisher publisher;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        return new KafkaEventListenerProvider(publisher, keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {
        try {
            String bootstrapServers = System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENV);

            if (bootstrapServers == null || bootstrapServers.isEmpty()) {
                LOG.error("Kafka bootstrap servers not configured! Set KC_KAFKA_BOOTSTRAP_SERVERS env variable.");
                throw new IllegalStateException("Kafka bootstrap servers must be configured");
            }

            LOG.infof("Initializing Kafka Event Listener Factory. Servers: %s", bootstrapServers);
            LOG.info("Event-to-Topic mapping:");
            LOG.info("  - REGISTER        → events.keycloak.UserRegistered");
            LOG.info("  - LOGIN           → events.keycloak.UserLogin");
            LOG.info("  - VERIFY_EMAIL    → events.keycloak.EmailVerified");

            publisher = new KafkaEventPublisher(bootstrapServers);

            LOG.info("Kafka Event Listener Factory initialized successfully");

        } catch (Exception e) {
            LOG.error("Failed to initialize Kafka Event Listener Factory", e);
            throw new RuntimeException("Kafka Event Listener initialization failed", e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        LOG.info("Post-initialization completed");
    }

    @Override
    public void close() {
        LOG.info("Closing Kafka Event Listener Factory");

        if (publisher != null) {
            try {
                publisher.close();
                LOG.info("Kafka publisher closed successfully");
            } catch (Exception e) {
                LOG.error("Error closing Kafka publisher", e);
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }
}