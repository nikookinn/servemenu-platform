package com.servemenu.userservice.infrastructure.keycloak;

import com.servemenu.userservice.application.dto.command.CreateCustomerCommand;
import com.servemenu.userservice.application.dto.command.CreateStoreUserCommand;
import com.servemenu.userservice.common.exception.KeycloakException;
import com.servemenu.userservice.domain.enums.StoreAccessLevel;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KeycloakUserManagementService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserManagementService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String realm;

    public KeycloakUserManagementService(Keycloak keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Create Store User in Keycloak with retry mechanism
     */
    @Retryable(
            retryFor = KeycloakException.class,
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UUID createStoreUserInKeycloak(CreateStoreUserCommand command) {
        log.debug("Creating store user in Keycloak: email={}", command.email());

        try {
            UsersResource usersResource = keycloakAdminClient.realm(realm).users();

            // Build user representation
            UserRepresentation userRep = buildUserRepresentation(
                    command.email(),
                    command.firstName(),
                    command.lastName(),
                    command.password()
            );

            // Create user
            UUID keycloakUserId = createUserInKeycloak(usersResource, userRep, command.email());

            // Assign role
            String roleName = command.accessLevel() == StoreAccessLevel.STORE_ADMIN
                    ? "store_admin"
                    : "store_user";
            assignRoleToUser(keycloakUserId.toString(), roleName);

            log.info("Store user created successfully in Keycloak: keycloakUserId={}, email={}, role={}",
                    keycloakUserId, command.email(), roleName);

            return keycloakUserId;

        } catch (ClientErrorException e) {
            handleClientError(e, "store user", command.email());
            throw new KeycloakException("Failed to create store user", e);
        } catch (Exception e) {
            log.error("Unexpected error creating store user in Keycloak: email={}", command.email(), e);
            throw new KeycloakException("Unexpected error creating store user in Keycloak", e);
        }
    }

    /**
     * Create Customer in Keycloak with retry mechanism
     */
    @Retryable(
            retryFor = KeycloakException.class,
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UUID createCustomerInKeycloak(CreateCustomerCommand command) {
        log.debug("Creating customer in Keycloak: email={}", command.email());

        try {
            UsersResource usersResource = keycloakAdminClient.realm(realm).users();

            UserRepresentation userRep = buildUserRepresentation(
                    command.email(),
                    command.firstName(),
                    command.lastName(),
                    command.password()
            );

            // Set customer-specific attributes
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("createdByBusinessId", List.of(command.createdByBusinessId().toString()));
            attributes.put("phoneNumber", List.of(command.phoneNumber()));
            attributes.put("countryCode", List.of(command.countryCode()));
            userRep.setAttributes(attributes);

            // Create user
            UUID keycloakUserId = createUserInKeycloak(usersResource, userRep, command.email());

            // Assign CUSTOMER role
            assignRoleToUser(keycloakUserId.toString(), "customer");

            log.info("Customer created successfully in Keycloak: keycloakUserId={}, email={}",
                    keycloakUserId, command.email());

            return keycloakUserId;

        } catch (ClientErrorException e) {
            handleClientError(e, "customer", command.email());
            throw new KeycloakException("Failed to create customer", e);
        } catch (Exception e) {
            log.error("Unexpected error creating customer in Keycloak: email={}", command.email(), e);
            throw new KeycloakException("Unexpected error creating customer in Keycloak", e);
        }
    }

    /**
     * Update user in Keycloak
     */
    public void updateUserInKeycloak(UUID keycloakUserId, String firstName, String lastName) {
        log.debug("Updating user in Keycloak: keycloakUserId={}", keycloakUserId);

        try {
            UserRepresentation userRep = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            if (userRep == null) {
                throw new KeycloakException("User not found in Keycloak: " + keycloakUserId);
            }

            userRep.setFirstName(firstName);
            userRep.setLastName(lastName);

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(userRep);

            log.info("User updated successfully in Keycloak: keycloakUserId={}", keycloakUserId);

        } catch (ClientErrorException e) {
            handleClientError(e, "user update", keycloakUserId.toString());
            throw new KeycloakException("Failed to update user in Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error updating user in Keycloak: keycloakUserId={}", keycloakUserId, e);
            throw new KeycloakException("Unexpected error updating user in Keycloak", e);
        }
    }

    /**
     * Set userId attribute in Keycloak user
     * This is used to link Keycloak user with our database user
     */
    public void setUserIdAttribute(UUID keycloakUserId, UUID userId) {
        log.debug("Setting userId attribute in Keycloak: keycloakUserId={}, userId={}", 
            keycloakUserId, userId);

        try {
            UserRepresentation userRep = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            if (userRep == null) {
                throw new KeycloakException("User not found in Keycloak: " + keycloakUserId);
            }

            // Set userId attribute
            Map<String, List<String>> attributes = userRep.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put("userId", List.of(userId.toString()));
            userRep.setAttributes(attributes);

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(userRep);

            log.info("userId attribute set successfully in Keycloak: keycloakUserId={}, userId={}", 
                keycloakUserId, userId);

        } catch (ClientErrorException e) {
            handleClientError(e, "set userId attribute", keycloakUserId.toString());
            throw new KeycloakException("Failed to set userId attribute in Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error setting userId attribute in Keycloak: keycloakUserId={}", 
                keycloakUserId, e);
            throw new KeycloakException("Unexpected error setting userId attribute in Keycloak", e);
        }
    }

    /**
     * Update user email in Keycloak
     * Note: Email verification will be required after update
     */
    @Retryable(
            retryFor = KeycloakException.class,
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void updateUserEmail(UUID keycloakUserId, String newEmail) {
        log.debug("Updating user email in Keycloak: keycloakUserId={}, newEmail={}", 
            keycloakUserId, newEmail);

        try {
            UserRepresentation userRep = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            if (userRep == null) {
                throw new KeycloakException("User not found in Keycloak: " + keycloakUserId);
            }

            // Update email only (username is read-only in Keycloak)
            userRep.setEmail(newEmail);
            userRep.setEmailVerified(false); // Require re-verification

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(userRep);

            log.info("User email updated successfully in Keycloak: keycloakUserId={}, newEmail={}", 
                keycloakUserId, newEmail);

            // TODO: Trigger email verification email from Keycloak
            // keycloakAdminClient.realm(realm).users().get(keycloakUserId.toString()).sendVerifyEmail();

        } catch (ClientErrorException e) {
            if (e.getResponse().getStatus() == 409) {
                throw new KeycloakException("Email already exists in Keycloak: " + newEmail, e);
            }
            handleClientError(e, "user email update", keycloakUserId.toString());
            throw new KeycloakException("Failed to update user email in Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error updating user email in Keycloak: keycloakUserId={}", 
                keycloakUserId, e);
            throw new KeycloakException("Unexpected error updating user email in Keycloak", e);
        }
    }

    /**
     * Enable user in Keycloak
     */
    public void enableUserInKeycloak(UUID keycloakUserId) {
        log.debug("Enabling user in Keycloak: keycloakUserId={}", keycloakUserId);

        try {
            UserRepresentation userRep = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            if (userRep == null) {
                throw new KeycloakException("User not found in Keycloak: " + keycloakUserId);
            }

            userRep.setEnabled(true);

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(userRep);

            log.info("User enabled successfully in Keycloak: keycloakUserId={}", keycloakUserId);

        } catch (ClientErrorException e) {
            handleClientError(e, "user enable", keycloakUserId.toString());
            throw new KeycloakException("Failed to enable user in Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error enabling user in Keycloak: keycloakUserId={}", keycloakUserId, e);
            throw new KeycloakException("Unexpected error enabling user in Keycloak", e);
        }
    }

    /**
     * Disable user in Keycloak
     */
    public void disableUserInKeycloak(UUID keycloakUserId) {
        log.debug("Disabling user in Keycloak: keycloakUserId={}", keycloakUserId);

        try {
            UserRepresentation userRep = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            if (userRep == null) {
                throw new KeycloakException("User not found in Keycloak: " + keycloakUserId);
            }

            userRep.setEnabled(false);

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(userRep);

            log.info("User disabled successfully in Keycloak: keycloakUserId={}", keycloakUserId);

        } catch (ClientErrorException e) {
            handleClientError(e, "user disable", keycloakUserId.toString());
            throw new KeycloakException("Failed to disable user in Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error disabling user in Keycloak: keycloakUserId={}", keycloakUserId, e);
            throw new KeycloakException("Unexpected error disabling user in Keycloak", e);
        }
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteUserFromKeycloak(UUID keycloakUserId) {
        log.debug("Deleting user from Keycloak: keycloakUserId={}", keycloakUserId);

        try {
            Response response = keycloakAdminClient.realm(realm)
                    .users()
                    .delete(keycloakUserId.toString());

            if (response.getStatus() == 204) {
                log.info("User deleted successfully from Keycloak: keycloakUserId={}", keycloakUserId);
            } else if (response.getStatus() == 404) {
                log.warn("User not found in Keycloak for deletion: keycloakUserId={}", keycloakUserId);
            } else {
                String errorMessage = response.readEntity(String.class);
                log.error("Failed to delete user from Keycloak: status={}, error={}",
                        response.getStatus(), errorMessage);
                throw new KeycloakException("Failed to delete user: " + errorMessage);
            }

            response.close();

        } catch (ClientErrorException e) {
            handleClientError(e, "user deletion", keycloakUserId.toString());
            throw new KeycloakException("Failed to delete user from Keycloak", e);
        } catch (Exception e) {
            log.error("Unexpected error deleting user from Keycloak: keycloakUserId={}", keycloakUserId, e);
            throw new KeycloakException("Unexpected error deleting user from Keycloak", e);
        }
    }

    // ============ Private Helper Methods ============

    private UserRepresentation buildUserRepresentation(
            String email,
            String firstName,
            String lastName,
            String password
    ) {
        UserRepresentation userRep = new UserRepresentation();
        // Use UUID as username to avoid conflicts when email is reused
        // Username will be unique and immutable, while email can be changed
        userRep.setUsername(UUID.randomUUID().toString());
        userRep.setEmail(email);
        userRep.setFirstName(firstName);
        userRep.setLastName(lastName);
        userRep.setEnabled(true);
        userRep.setEmailVerified(false);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        userRep.setCredentials(List.of(credential));

        return userRep;
    }

    private UUID createUserInKeycloak(UsersResource usersResource, UserRepresentation userRep, String email) {
        try (Response response = usersResource.create(userRep)) {

            if (response.getStatus() == 201) {
                String locationHeader = response.getHeaderString("Location");
                if (locationHeader == null || locationHeader.isEmpty()) {
                    throw new KeycloakException("Location header missing in Keycloak response");
                }
                String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
                return UUID.fromString(keycloakUserId);
            } else if (response.getStatus() == 409) {
                throw new KeycloakException("User already exists with email: " + email);
            } else {
                String errorMessage = response.readEntity(String.class);
                log.error("Failed to create user in Keycloak: status={}, error={}",
                        response.getStatus(), errorMessage);
                throw new KeycloakException("Failed to create user: " + errorMessage);
            }
        }
    }

    public void assignRoleToUser(String keycloakUserId, String roleName) {
        try {
            log.info("Attempting to assign role '{}' to user: {}", roleName, keycloakUserId);

            RoleRepresentation role = keycloakAdminClient.realm(realm)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            log.info("Role '{}' found in Keycloak: id={}", roleName, role != null ? role.getId() : "null");

            if (role == null) {
                throw new KeycloakException("Role not found in Keycloak: " + roleName);
            }

            log.info("Assigning role to user via Keycloak API...");
            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));

            log.info("Role '{}' assigned successfully to user: {}", roleName, keycloakUserId);

        } catch (jakarta.ws.rs.ForbiddenException e) {
            log.error("403 FORBIDDEN - Insufficient permissions to assign role '{}' to user: {}. " +
                    "Token may not have 'manage-users' role.", roleName, keycloakUserId, e);
            log.error("Full exception details:", e);
            throw new KeycloakException("Forbidden: Cannot assign role due to insufficient permissions", e);

        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user: {}. Error type: {}",
                    roleName, keycloakUserId, e.getClass().getName(), e);
            throw new KeycloakException("Failed to assign role to user", e);
        }
    }

    /**
     * Remove role from user in Keycloak
     */
    public void removeRoleFromUser(String keycloakUserId, String roleName) {
        try {
            log.info("Attempting to remove role '{}' from user: {}", roleName, keycloakUserId);

            RoleRepresentation role = keycloakAdminClient.realm(realm)
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            if (role == null) {
                log.warn("Role '{}' not found in Keycloak, skipping removal", roleName);
                return;
            }

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .remove(List.of(role));

            log.info("Role '{}' removed successfully from user: {}", roleName, keycloakUserId);

        } catch (Exception e) {
            log.error("Failed to remove role '{}' from user: {}", roleName, keycloakUserId, e);
            throw new KeycloakException("Failed to remove role from user", e);
        }
    }

    /**
     * Update user's store access level (change role from store_user to store_admin or vice versa)
     */
    public void updateStoreUserRole(UUID keycloakUserId, StoreAccessLevel newAccessLevel, StoreAccessLevel oldAccessLevel) {
        log.debug("Updating store user role in Keycloak: keycloakUserId={}, oldRole={}, newRole={}", 
            keycloakUserId, oldAccessLevel, newAccessLevel);

        try {
            String oldRoleName = oldAccessLevel == StoreAccessLevel.STORE_ADMIN ? "store_admin" : "store_user";
            String newRoleName = newAccessLevel == StoreAccessLevel.STORE_ADMIN ? "store_admin" : "store_user";

            if (oldRoleName.equals(newRoleName)) {
                log.debug("Role unchanged, skipping update");
                return;
            }

            // Remove old role
            removeRoleFromUser(keycloakUserId.toString(), oldRoleName);

            // Assign new role
            assignRoleToUser(keycloakUserId.toString(), newRoleName);

            log.info("Store user role updated successfully in Keycloak: keycloakUserId={}, oldRole={}, newRole={}", 
                keycloakUserId, oldRoleName, newRoleName);

        } catch (Exception e) {
            log.error("Failed to update store user role in Keycloak: keycloakUserId={}", keycloakUserId, e);
            throw new KeycloakException("Failed to update user role in Keycloak", e);
        }
    }

    private void handleClientError(ClientErrorException e, String operation, String identifier) {
        int status = e.getResponse().getStatus();
        String errorMessage = e.getMessage();
        
        // Try to read response body for detailed error message
        String responseBody = null;
        try {
            if (e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
        } catch (Exception ex) {
            log.warn("Failed to read error response body", ex);
        }

        switch (status) {
            case 400 -> log.error("Bad request for {}: identifier={}, error={}, responseBody={}", 
                operation, identifier, errorMessage, responseBody);
            case 401 -> log.error("Unauthorized access for {}: identifier={}", operation, identifier);
            case 403 -> log.error("Forbidden access for {}: identifier={}", operation, identifier);
            case 404 -> log.error("Resource not found for {}: identifier={}", operation, identifier);
            case 409 -> log.error("Conflict for {}: identifier={}, error={}, responseBody={}", 
                operation, identifier, errorMessage, responseBody);
            default -> log.error("Client error for {}: status={}, identifier={}, error={}, responseBody={}",
                    operation, status, identifier, errorMessage, responseBody);
        }
    }

    public void updateUserAttribute(UUID keycloakUserId, String attributeName, String value) {
        try {
            UserRepresentation user = keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .toRepresentation();

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            attributes.put(attributeName, List.of(value));
            user.setAttributes(attributes);

            keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId.toString())
                    .update(user);

            log.info("Updated Keycloak user attribute: keycloakUserId={}, {}={}",
                    keycloakUserId, attributeName, value);

        } catch (Exception e) {
            log.error("Failed to update Keycloak user attribute", e);
            throw new KeycloakException("Failed to update user attribute", e);
        }
    }
}