package com.servemenu.userservice.application.service;

import com.servemenu.userservice.application.dto.command.CreateCustomerCommand;
import com.servemenu.userservice.application.dto.response.CustomerResponse;
import com.servemenu.userservice.application.mapper.UserMapper;
import com.servemenu.userservice.common.exception.BusinessException;
import com.servemenu.userservice.common.exception.DuplicateResourceException;
import com.servemenu.userservice.common.exception.ResourceNotFoundException;
import com.servemenu.userservice.domain.enums.UserType;
import com.servemenu.userservice.domain.event.CustomerCreatedEvent;
import com.servemenu.userservice.domain.model.CustomerProfile;
import com.servemenu.userservice.domain.model.User;
import com.servemenu.userservice.domain.model.UserPreferences;
import com.servemenu.userservice.domain.repository.CustomerProfileRepository;
import com.servemenu.userservice.domain.repository.UserPreferencesRepository;
import com.servemenu.userservice.domain.repository.UserRepository;
import com.servemenu.userservice.infrastructure.kafka.producer.DomainEventPublisher;
import com.servemenu.userservice.infrastructure.keycloak.KeycloakUserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final KeycloakUserManagementService keycloakService;
    private final DomainEventPublisher eventPublisher;
    private final UserMapper userMapper;

    public CustomerService(
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            UserPreferencesRepository preferencesRepository,
            KeycloakUserManagementService keycloakService,
            DomainEventPublisher eventPublisher,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.preferencesRepository = preferencesRepository;
        this.keycloakService = keycloakService;
        this.eventPublisher = eventPublisher;
        this.userMapper = userMapper;
    }

    @Transactional
    public CustomerResponse registerCustomer(CreateCustomerCommand command) {
        log.debug("Registering customer: email={}", command.email());

        // Validate email
        if (command.email() == null || command.email().isBlank()) {
            throw new BusinessException("Email cannot be empty");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateResourceException("Customer already exists with email: " + command.email());
        }

        // Validate phone number
        if (command.phoneNumber() == null || command.phoneNumber().isBlank()) {
            throw new BusinessException("Phone number cannot be empty");
        }

        // Create customer in Keycloak
        UUID keycloakUserId = keycloakService.createCustomerInKeycloak(command);

        User user = new User();
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(command.email());
        user.setFirstName(command.firstName());
        user.setLastName(command.lastName());
        user.setEmailVerified(false);
        user.setUserType(UserType.CUSTOMER);

        User savedUser = userRepository.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(savedUser);
        profile.setPhoneNumber(command.phoneNumber());
        profile.setCountryCode(command.countryCode());
        profile.setCreatedByBusinessId(command.createdByBusinessId());
        profile.setLoyaltyPoints(0);

        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        UserPreferences preferences = new UserPreferences();
        preferences.setUser(savedUser);
        preferencesRepository.save(preferences);

        CustomerCreatedEvent event = new CustomerCreatedEvent(
                savedUser.getId(),
                savedUser.getKeycloakUserId(),
                savedUser.getEmail(),
                command.createdByBusinessId()
        );
        eventPublisher.publish(event);

        log.info("Customer registration initiated in Keycloak: email={}, keycloakUserId={}",
                command.email(), keycloakUserId);

        return userMapper.toCustomerResponse(savedProfile);
    }

    @Transactional
    public CustomerResponse updateCustomerProfile(UUID userId, String firstName, String lastName) {
        log.debug("Updating customer profile: userId={}", userId);

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        User user = profile.getUser();

        // Validate user is not deleted
        if (user.isDeleted()) {
            throw new BusinessException("Cannot update deleted user profile");
        }

        user.updateProfile(firstName, lastName);
        userRepository.save(user);

        log.info("Customer profile updated: userId={}", userId);

        return userMapper.toCustomerResponse(profile);
    }

    @Transactional
    public void addLoyaltyPoints(UUID userId, Integer points) {
        log.debug("Adding loyalty points: userId={}, points={}", userId, points);

        if (points == null || points <= 0) {
            throw new BusinessException("Points must be positive");
        }

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        profile.addLoyaltyPoints(points);
        customerProfileRepository.save(profile);

        log.info("Loyalty points added: userId={}, points={}, total={}",
                userId, points, profile.getLoyaltyPoints());
    }

    @Transactional
    public void redeemLoyaltyPoints(UUID userId, Integer points) {
        log.debug("Redeeming loyalty points: userId={}, points={}", userId, points);

        if (points == null || points <= 0) {
            throw new BusinessException("Points must be positive");
        }

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // This will throw exception if insufficient points
        profile.redeemLoyaltyPoints(points);
        customerProfileRepository.save(profile);

        log.info("Loyalty points redeemed: userId={}, points={}, remaining={}",
                userId, points, profile.getLoyaltyPoints());
    }

    public CustomerResponse findByUserId(UUID userId) {
        log.debug("Finding customer by userId: {}", userId);

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return userMapper.toCustomerResponse(profile);
    }

    public CustomerResponse findByKeycloakUserId(UUID keycloakUserId) {
        log.debug("Finding customer by keycloakUserId: {}", keycloakUserId);

        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        CustomerProfile profile = customerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        return userMapper.toCustomerResponse(profile);
    }

    public Page<CustomerResponse> findByBusinessId(UUID businessId, Pageable pageable) {
        log.debug("Finding customers by businessId: {}, page: {}", businessId, pageable.getPageNumber());

        Page<CustomerProfile> profiles = customerProfileRepository.findByBusinessId(businessId, pageable);
        return profiles.map(userMapper::toCustomerResponse);
    }

    public long countByBusinessId(UUID businessId) {
        log.debug("Counting customers by businessId: {}", businessId);

        return customerProfileRepository.countByBusinessId(businessId);
    }
}