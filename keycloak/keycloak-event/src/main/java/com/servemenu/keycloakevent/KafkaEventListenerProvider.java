package com.servemenu.keycloakevent;

import com.servemenu.keycloakevent.dto.EmailVerificationEventDTO;
import com.servemenu.keycloakevent.dto.KeycloakEventDTO;
import com.servemenu.keycloakevent.dto.UserInfo;
import com.servemenu.keycloakevent.publisher.KafkaEventPublisher;
import com.servemenu.keycloakevent.utils.EventPublisherTransaction;
import com.servemenu.keycloakevent.utils.JsonUtils;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

public class KafkaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(KafkaEventListenerProvider.class);
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private static final String BUSINESS_FRONTEND_CLIENT = "business-frontend";
    private static final String USER_REGISTERED_TOPIC = "events.keycloak.UserRegistered";
    private static final String EMAIL_VERIFIED_TOPIC = "events.keycloak.EmailVerified";

    private static final String EVENT_TYPE_USER_REGISTERED = "UserRegistered";
    private static final String EVENT_TYPE_EMAIL_VERIFIED = "EmailVerified";

    private final KafkaEventPublisher publisher;
    private final KeycloakSession session;
    private final EventPublisherTransaction publisherTransaction;

    public KafkaEventListenerProvider(KafkaEventPublisher publisher, KeycloakSession session) {
        this.publisher = publisher;
        this.session = session;

        this.publisherTransaction = new EventPublisherTransaction();
        this.publisherTransaction.begin();

        session.getTransactionManager().enlistAfterCompletion(publisherTransaction);
    }

    @Override
    public void onEvent(Event event) {
        if (event == null || event.getType() == null) {
            return;
        }

        try {
            if (event.getType() == EventType.REGISTER) {
                handleUserRegistration(event);
            } else if (event.getType() == EventType.VERIFY_EMAIL) {
                handleEmailVerification(event);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process event: type=%s, userId=%s",
                    event.getType(), event.getUserId());
        }
    }

    /**
     * Handle business owner registration (only from business-frontend)
     */
    private void handleUserRegistration(Event event) {
        if (!BUSINESS_FRONTEND_CLIENT.equals(event.getClientId())) {
            LOG.debugf("Ignoring REGISTER from clientId: %s", event.getClientId());
            return;
        }

        UserModel user = validateEventAndGetUser(event);
        if (user == null) {
            return;
        }

        publisherTransaction.addAfterCommitAction(() -> {
            try {
                publishBusinessOwnerRegistration(event);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to publish registration event: userId=%s", event.getUserId());
            }
        });

        LOG.infof("Business owner registration queued: userId=%s, email=%s",
                event.getUserId(), user.getEmail());
    }

    /**
     * Handle email verification
     */
    private void handleEmailVerification(Event event) {
        UserModel user = validateEventAndGetUser(event);
        if (user == null) {
            return;
        }

        if (!user.isEmailVerified()) {
            LOG.debugf("Email not verified yet: userId=%s", event.getUserId());
            return;
        }

        publisherTransaction.addAfterCommitAction(() -> {
            try {
                RealmModel realm = session.realms().getRealm(event.getRealmId());
                UserModel userAfterCommit = getUserFromRealm(realm, event.getUserId());

                if (userAfterCommit != null && userAfterCommit.isEmailVerified()) {
                    publishEmailVerification(event);
                } else {
                    LOG.warnf("Email verification status changed after commit: userId=%s", event.getUserId());
                }
            } catch (Exception e) {
                LOG.errorf(e, "Failed to publish email verification event: userId=%s", event.getUserId());
            }
        });

        LOG.infof("Email verification queued: userId=%s", event.getUserId());
    }

    /**
     * Validate event and get user
     * Returns null if validation fails
     */
    private UserModel validateEventAndGetUser(Event event) {
        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            LOG.warnf("Realm not found: %s", event.getRealmId());
            return null;
        }

        UserModel user = getUserFromRealm(realm, event.getUserId());
        if (user == null) {
            LOG.warnf("User not found: %s", event.getUserId());
            return null;
        }

        return user;
    }

    /**
     * Get user from realm
     */
    private UserModel getUserFromRealm(RealmModel realm, String userId) {
        if (realm == null || userId == null) {
            return null;
        }
        return session.users().getUserById(realm, userId);
    }

    /**
     * Publish business owner registration to Kafka
     */
    private void publishBusinessOwnerRegistration(Event event) throws Exception {
        RealmModel realm = session.realms().getRealm(event.getRealmId());
        UserModel user = getUserFromRealm(realm, event.getUserId());

        if (realm == null || user == null) {
            LOG.warnf("Realm or user not found after commit: realmId=%s, userId=%s",
                    event.getRealmId(), event.getUserId());
            return;
        }

        KeycloakEventDTO eventDTO = buildUserRegisteredEvent(event, realm, user);
        String json = JsonUtils.MAPPER.writeValueAsString(eventDTO);

        publisher.publish(
                USER_REGISTERED_TOPIC,
                user.getId(),
                json,
                "USER_REGISTERED"
        );

        LOG.infof("Published USER_REGISTERED: userId=%s, email=%s", user.getId(), user.getEmail());
    }

    /**
     * Build USER_REGISTERED event DTO
     */
    private KeycloakEventDTO buildUserRegisteredEvent(Event event, RealmModel realm, UserModel user) {
        KeycloakEventDTO eventDTO = new KeycloakEventDTO();
        eventDTO.setEventId(UUID.randomUUID().toString());
        eventDTO.setEventType(EVENT_TYPE_USER_REGISTERED);
        eventDTO.setTimestamp(formatTimestamp(event.getTime()));
        eventDTO.setRealm(realm.getName());
        eventDTO.setClientId(event.getClientId());

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setEmailVerified(user.isEmailVerified());
        userInfo.setEnabled(user.isEnabled());
        userInfo.setRoles(Set.of());
        userInfo.setAttributes(null);

        eventDTO.setUser(userInfo);
        return eventDTO;
    }

    /**
     * Publish email verification to Kafka
     */
    private void publishEmailVerification(Event event) throws Exception {
        EmailVerificationEventDTO eventDTO = new EmailVerificationEventDTO(
                UUID.randomUUID().toString(),
                EVENT_TYPE_EMAIL_VERIFIED,
                formatTimestamp(event.getTime()),
                event.getUserId(),
                true
        );
        String json = JsonUtils.MAPPER.writeValueAsString(eventDTO);

        publisher.publish(
                EMAIL_VERIFIED_TOPIC,
                event.getUserId(),
                json,
                "EMAIL_VERIFIED"
        );
        LOG.infof("Published EMAIL_VERIFIED: userId=%s", event.getUserId());
    }

    private String formatTimestamp(long timestamp) {
        return ISO_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }
}