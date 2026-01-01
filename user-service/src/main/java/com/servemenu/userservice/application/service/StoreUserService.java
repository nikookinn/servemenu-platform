package com.servemenu.userservice.application.service;

import com.servemenu.userservice.application.dto.command.CreateStoreUserCommand;
import com.servemenu.userservice.application.dto.request.UpdateStoreUserRequest;
import com.servemenu.userservice.application.dto.response.PageResponse;
import com.servemenu.userservice.application.dto.response.StoreUserResponse;
import com.servemenu.userservice.application.mapper.UserMapper;
import com.servemenu.userservice.common.exception.DuplicateResourceException;
import com.servemenu.userservice.common.exception.KeycloakException;
import com.servemenu.userservice.common.exception.ResourceNotFoundException;
import com.servemenu.userservice.common.exception.UnauthorizedException;
import com.servemenu.userservice.domain.enums.UserType;
import com.servemenu.userservice.domain.event.StoreUserCreatedEvent;
import com.servemenu.userservice.domain.model.BusinessOwnerProfile;
import com.servemenu.userservice.domain.model.StoreUserProfile;
import com.servemenu.userservice.domain.model.User;
import com.servemenu.userservice.domain.model.UserPreferences;
import com.servemenu.userservice.domain.repository.BusinessOwnerProfileRepository;
import com.servemenu.userservice.domain.repository.StoreUserProfileRepository;
import com.servemenu.userservice.domain.repository.UserPreferencesRepository;
import com.servemenu.userservice.domain.repository.UserRepository;
import com.servemenu.userservice.infrastructure.grpc.BusinessGrpcClient;
import com.servemenu.userservice.infrastructure.kafka.producer.DomainEventPublisher;
import com.servemenu.userservice.infrastructure.keycloak.KeycloakUserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StoreUserService {

    private static final Logger log = LoggerFactory.getLogger(StoreUserService.class);
    private final UserRepository userRepository;
    private final StoreUserProfileRepository storeUserProfileRepository;
    private final BusinessOwnerProfileRepository businessOwnerProfileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final KeycloakUserManagementService keycloakService;
    private final DomainEventPublisher eventPublisher;
    private final UserMapper userMapper;
    private final BusinessGrpcClient businessGrpcClient;

    public StoreUserService(
            UserRepository userRepository,
            StoreUserProfileRepository storeUserProfileRepository,
            BusinessOwnerProfileRepository businessOwnerProfileRepository,
            UserPreferencesRepository preferencesRepository,
            KeycloakUserManagementService keycloakService,
            DomainEventPublisher eventPublisher,
            UserMapper userMapper,
            BusinessGrpcClient businessGrpcClient
    ) {
        this.userRepository = userRepository;
        this.storeUserProfileRepository = storeUserProfileRepository;
        this.businessOwnerProfileRepository = businessOwnerProfileRepository;
        this.preferencesRepository = preferencesRepository;
        this.keycloakService = keycloakService;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
        this.businessGrpcClient = businessGrpcClient;
    }

    @Transactional
    public StoreUserResponse createStoreUser(CreateStoreUserCommand command, UUID currentUserId) {
        // Permission validation
        validateStoreUserCreationPermission(currentUserId, command.businessId(), command.storeId());

        // Check if email already exists
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateResourceException("User already exists with email: " + command.email());
        }

        try {
            // Create user in Keycloak
            UUID keycloakUserId = keycloakService.createStoreUserInKeycloak(command);

            User user = new User();
            user.setKeycloakUserId(keycloakUserId);
            user.setEmail(command.email());
            user.setFirstName(command.firstName());
            user.setLastName(command.lastName());
            user.setEmailVerified(false);
            user.setUserType(UserType.STORE_USER);

            User savedUser = userRepository.save(user);

            // Set userId attribute in Keycloak for JWT claims
            keycloakService.setUserIdAttribute(keycloakUserId, savedUser.getId());

            StoreUserProfile profile = new StoreUserProfile();
            profile.setUser(savedUser);
            profile.setBusinessId(command.businessId());
            profile.setStoreId(command.storeId());
            profile.setAccessLevel(command.accessLevel());
            profile.setActive(true);
            profile.setCreatedByUserId(command.createdByUserId());

            StoreUserProfile savedProfile = storeUserProfileRepository.save(profile);

            UserPreferences preferences = new UserPreferences();
            preferences.setUser(savedUser);
            preferencesRepository.save(preferences);

            StoreUserCreatedEvent event = new StoreUserCreatedEvent(
                    savedUser.getId(),
                    savedUser.getKeycloakUserId(),
                    command.businessId(),
                    command.storeId(),
                    profile.getAccessLevel(),
                    command.createdByUserId()
            );

            eventPublisher.publish(event);

            log.info("Store user creation initiated in Keycloak: email={}, keycloakUserId={}",
                    command.email(), keycloakUserId);



            return userMapper.toStoreUserResponse(savedProfile);
        } catch (KeycloakException e) {
            log.error("Failed to create store user in Keycloak", e);
            throw e;
        }
    }

    @Transactional
    public StoreUserResponse updateStoreUser(UUID userId, UpdateStoreUserRequest request) {
        log.debug("Updating store user: userId={}", userId);
        
        StoreUserProfile profile = storeUserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Store user not found"));

        User user = profile.getUser();
        
        // Update basic profile (firstName, lastName)
        if (request.firstName() != null || request.lastName() != null) {
            String newFirstName = request.firstName() != null ? request.firstName() : user.getFirstName();
            String newLastName = request.lastName() != null ? request.lastName() : user.getLastName();
            
            try {
                // Update in Keycloak first
                keycloakService.updateUserInKeycloak(user.getKeycloakUserId(), newFirstName, newLastName);
                log.info("User profile updated in Keycloak: userId={}, firstName={}, lastName={}", 
                    userId, newFirstName, newLastName);
                
                // Then update in database
                user.updateProfile(newFirstName, newLastName);
                
            } catch (KeycloakException e) {
                log.error("Failed to update user profile in Keycloak: userId={}", userId, e);
                throw new KeycloakException("Failed to update user profile. Please try again later.", e);
            }
        }
        
        // Update email if changed (sync with Keycloak)
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            log.info("Updating store user email: userId={}, oldEmail={}, newEmail={}", 
                userId, user.getEmail(), request.email());
            
            // Check if new email already exists
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email already exists: " + request.email());
            }
            
            try {
                // Update in Keycloak first
                keycloakService.updateUserEmail(user.getKeycloakUserId(), request.email());
                
                // Update in our database
                user.setEmail(request.email());
                user.setEmailVerified(false); // Require re-verification
                
                log.info("Store user email updated successfully: userId={}, newEmail={}", 
                    userId, request.email());
                
            } catch (KeycloakException e) {
                log.error("Failed to update email in Keycloak: userId={}", userId, e);
                throw new KeycloakException("Failed to update email. Please try again later.", e);
            }
        }
        
        userRepository.save(user);

        // Update access level if provided
        if (request.accessLevel() != null && request.accessLevel() != profile.getAccessLevel()) {
            try {
                // Update role in Keycloak first
                keycloakService.updateStoreUserRole(
                    user.getKeycloakUserId(), 
                    request.accessLevel(), 
                    profile.getAccessLevel()
                );
                log.info("User role updated in Keycloak: userId={}, oldRole={}, newRole={}", 
                    userId, profile.getAccessLevel(), request.accessLevel());
                
                // Then update in database
                profile.changeAccessLevel(request.accessLevel());
                
            } catch (KeycloakException e) {
                log.error("Failed to update user role in Keycloak: userId={}", userId, e);
                throw new KeycloakException("Failed to update user role. Please try again later.", e);
            }
        }

        // Update active status if provided
        if (request.isActive() != null) {
            try {
                if (Boolean.TRUE.equals(request.isActive())) {
                    // Enable in Keycloak first
                    keycloakService.enableUserInKeycloak(user.getKeycloakUserId());
                    log.info("User enabled in Keycloak: userId={}, keycloakUserId={}", userId, user.getKeycloakUserId());
                    
                    // Then activate in database
                    user.activate(); // Activate user account
                    profile.activate(); // Activate store user profile
                } else {
                    // Disable in Keycloak first
                    keycloakService.disableUserInKeycloak(user.getKeycloakUserId());
                    log.info("User disabled in Keycloak: userId={}, keycloakUserId={}", userId, user.getKeycloakUserId());
                    
                    // Then deactivate in database
                    user.suspend(); // Suspend user account
                    profile.deactivate(); // Deactivate store user profile
                }
            } catch (KeycloakException e) {
                log.error("Failed to update user status in Keycloak: userId={}", userId, e);
                throw new KeycloakException("Failed to update user status. Please try again later.", e);
            }
        }

        StoreUserProfile updatedProfile = storeUserProfileRepository.save(profile);

        log.info("Store user updated: userId={}", userId);

        return userMapper.toStoreUserResponse(updatedProfile);
    }

    @Transactional
    public void deactivateStoreUser(UUID userId) {
        log.debug("Deactivating store user: userId={}", userId);
        
        StoreUserProfile profile = storeUserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Store user not found"));

        User user = profile.getUser();
        String originalEmail = user.getEmail();
        
        try {
            // 1. Anonymize email to free it up for reuse
            // Username is already UUID, so no conflict when email is reused
            String anonymizedEmail = userId.toString().replace("-", "") + "@deleted.local";
            
            // 2. Update email in Keycloak (to free up the original email)
            keycloakService.updateUserEmail(user.getKeycloakUserId(), anonymizedEmail);
            log.info("User email anonymized in Keycloak: userId={}, originalEmail={}, anonymizedEmail={}", 
                userId, originalEmail, anonymizedEmail);
            
            // 3. Disable user in Keycloak
            keycloakService.disableUserInKeycloak(user.getKeycloakUserId());
            log.info("User disabled in Keycloak: userId={}, keycloakUserId={}", userId, user.getKeycloakUserId());
            
            // 4. Update email in database
            user.setEmail(anonymizedEmail);
            
            // 5. Soft delete user account (sets DELETED status and deleted_at timestamp)
            user.softDelete();
            userRepository.save(user);
            
            // 6. Deactivate store user profile
            profile.deactivate();
            storeUserProfileRepository.save(profile);
            
            log.info("Store user deactivated successfully: userId={}, originalEmail={}, anonymizedEmail={}, accountStatus={}", 
                userId, originalEmail, anonymizedEmail, user.getAccountStatus());
            
        } catch (KeycloakException e) {
            log.error("Failed to disable user in Keycloak: userId={}", userId, e);
            throw new KeycloakException("Failed to deactivate user. Please try again later.", e);
        }
    }

    public PageResponse<StoreUserResponse> findByStoreId(UUID storeId, Pageable pageable) {
        Page<StoreUserProfile> profiles = storeUserProfileRepository.findActiveByStoreId(storeId, pageable);
        Page<StoreUserResponse> responsePage = profiles.map(userMapper::toStoreUserResponse);
        return PageResponse.of(responsePage);
    }

    public StoreUserResponse findByUserId(UUID userId) {
        StoreUserProfile profile = storeUserProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Store user not found"));

        return userMapper.toStoreUserResponse(profile);
    }

    public long countActiveByStoreId(UUID storeId) {
        return storeUserProfileRepository.countActiveByStoreId(storeId);
    }

    /**
     * Validate store user creation permission using gRPC
     * - Business owner: Validate via gRPC that user owns the business
     * - Store admin: Validate via local DB that user is admin for the specific store
     */
    private void validateStoreUserCreationPermission(UUID currentUserId, UUID businessId, UUID storeId) {
        log.debug("Validating store user creation permission for user: {}, business: {}, store: {}", 
                currentUserId, businessId, storeId);

        // Check if current user is business owner
        Optional<BusinessOwnerProfile> ownerProfile = businessOwnerProfileRepository.findByUserId(currentUserId);
        if (ownerProfile.isPresent()) {
            log.debug("User is business owner, validating ownership via gRPC");
            
            // Validate business ownership via gRPC to business-service
            // Note: Using the User's ID (which matches businessOwnerId in Business entity)
            boolean isOwner = businessGrpcClient.validateBusinessOwnership(
                    ownerProfile.get().getUser().getId(),
                    businessId
            );
            
            if (isOwner) {
                log.debug("Business ownership validated successfully via gRPC");
                return;
            } else {
                log.warn("Business ownership validation failed via gRPC for businessOwnerId: {}, businessId: {}", 
                        ownerProfile.get().getId(), businessId);
                throw new UnauthorizedException("You do not own this business");
            }
        }

        // Check if current user is store admin for this specific store
        Optional<StoreUserProfile> storeUserProfile = storeUserProfileRepository.findByUserId(currentUserId);
        if (storeUserProfile.isPresent()) {
            StoreUserProfile profile = storeUserProfile.get();
            
            // Validate: same store + admin role + active
            if (profile.getStoreId().equals(storeId) 
                    && profile.isAdmin() 
                    && Boolean.TRUE.equals(profile.getActive())) {
                
                log.debug("User is store admin for the requested store");
                return;
            } else {
                log.warn("User is not admin or store mismatch. Profile store: {}, Requested store: {}", 
                        profile.getStoreId(), storeId);
            }
        }

        log.error("Permission validation failed for user: {}", currentUserId);
        throw new UnauthorizedException("You do not have permission to create store users");
    }
}