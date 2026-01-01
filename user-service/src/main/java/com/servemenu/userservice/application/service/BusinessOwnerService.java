package com.servemenu.userservice.application.service;

import com.servemenu.userservice.application.dto.response.BusinessOwnerResponse;
import com.servemenu.userservice.application.mapper.UserMapper;
import com.servemenu.userservice.common.exception.DuplicateResourceException;
import com.servemenu.userservice.common.exception.ResourceNotFoundException;
import com.servemenu.userservice.domain.enums.UserType;
import com.servemenu.userservice.domain.model.BusinessOwnerProfile;
import com.servemenu.userservice.domain.model.User;
import com.servemenu.userservice.domain.model.UserPreferences;
import com.servemenu.userservice.domain.repository.BusinessOwnerProfileRepository;
import com.servemenu.userservice.domain.repository.UserPreferencesRepository;
import com.servemenu.userservice.domain.repository.UserRepository;
import com.servemenu.userservice.infrastructure.kafka.producer.DomainEventPublisher;
import com.servemenu.userservice.domain.event.BusinessOwnerCreatedEvent;
import com.servemenu.userservice.infrastructure.keycloak.KeycloakUserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BusinessOwnerService {

    private static final Logger log = LoggerFactory.getLogger(BusinessOwnerService.class);
    private final UserRepository userRepository;
    private final BusinessOwnerProfileRepository profileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final DomainEventPublisher eventPublisher;
    private final KeycloakUserManagementService keycloakService;
    private final UserMapper userMapper;

    public BusinessOwnerService(
            UserRepository userRepository,
            BusinessOwnerProfileRepository profileRepository,
            UserPreferencesRepository preferencesRepository,
            DomainEventPublisher eventPublisher,
            KeycloakUserManagementService keycloakService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakService = keycloakService;
        this.userMapper = userMapper;
    }

    @Transactional
    public BusinessOwnerResponse createBusinessOwner(
            UUID keycloakUserId,
            String email,
            String firstName,
            String lastName,
            Boolean emailVerified
    ) {
        if (userRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new DuplicateResourceException("Business owner already exists with keycloakUserId: " + keycloakUserId);
        }

        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(emailVerified);
        user.setUserType(UserType.BUSINESS_OWNER);

        User savedUser = userRepository.save(user);

        BusinessOwnerProfile profile = new BusinessOwnerProfile();
        profile.setUser(savedUser);

        BusinessOwnerProfile savedProfile = profileRepository.save(profile);

        UserPreferences preferences = new UserPreferences();
        preferences.setUser(savedUser);
        preferencesRepository.save(preferences);

        try {
            keycloakService.assignRoleToUser(keycloakUserId.toString(), "business_owner");
            keycloakService.updateUserAttribute(
                    keycloakUserId,
                    "userId",
                    savedUser.getId().toString()
            );
            log.info("Assigned 'business_owner' role in Keycloak: keycloakUserId={}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to assign role in Keycloak, but user created in DB: keycloakUserId={}",
                    keycloakUserId, e);
            // Continue - scheduled job will fix this later
        }

        BusinessOwnerCreatedEvent event = new BusinessOwnerCreatedEvent(
                savedUser.getId(),
                savedUser.getKeycloakUserId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
        eventPublisher.publish(event);

        log.info("Business owner created: userId={}, keycloakUserId={}", savedUser.getId(), keycloakUserId);

        return userMapper.toBusinessOwnerResponse(savedProfile);
    }

    /**
     * Complete onboarding for business owner
     * Called when BusinessDetailsCompletedEvent is received from business-service
     * Sets onboardingCompleted = true and stores businessId locally
     * 
     * Note: No event is published here because this is a response to business-service's
     * BusinessDetailsCompletedEvent. We're just synchronizing local state.
     */
    @Transactional
    public void completeOnboarding(UUID userId, UUID businessId) {
        BusinessOwnerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business owner profile not found"));

        profile.completeOnboarding();
        profile.setBusinessId(businessId);
        profileRepository.save(profile);

        log.info("BusinessOwner onboarding synchronized: userId={}, businessId={}", userId, businessId);
    }

    public BusinessOwnerResponse findByUserId(UUID userId) {
        BusinessOwnerProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Business owner not found"));

        return userMapper.toBusinessOwnerResponse(profile);
    }

    public BusinessOwnerResponse findByKeycloakUserId(UUID keycloakUserId) {
        BusinessOwnerProfile profile = profileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Business owner not found"));

        return userMapper.toBusinessOwnerResponse(profile);
    }

    public boolean existsByKeycloakUserId(UUID keycloakUserId) {
        return userRepository.existsByKeycloakUserId(keycloakUserId);
    }

    public boolean existsByUserId(UUID userId) {
        return profileRepository.existsByUserId(userId);
    }
}