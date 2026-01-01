package com.servemenu.userservice.application.service;

import com.servemenu.userservice.application.dto.response.UserResponse;
import com.servemenu.userservice.application.mapper.UserMapper;
import com.servemenu.userservice.domain.model.User;
import com.servemenu.userservice.domain.repository.UserRepository;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    
    public UserResponse findByKeycloakUserId(UUID keycloakUserId) {
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with keycloakUserId: " + keycloakUserId));

        return userMapper.toUserResponse(user);
    }
    
    public UserResponse findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return userMapper.toUserResponse(user);
    }
    
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return userMapper.toUserResponse(user);
    }
    
    @Transactional
    public UserResponse updateProfile(UUID userId, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.updateProfile(firstName, lastName);
        User updatedUser = userRepository.save(user);

        log.info("User profile updated: userId={}", userId);

        // TODO: Optionally sync with Keycloak
        // This is not required since User Service is the source of truth for profile data
        // But can be enabled if you want Keycloak token to also have updated names
        // syncProfileWithKeycloak(user.getKeycloakUserId(), firstName, lastName);

        return userMapper.toUserResponse(updatedUser);
    }
    @Transactional
    public void verifyEmailByKeycloakUserId(UUID keycloakUserId) {
        log.debug("Verifying email by keycloakUserId: {}", keycloakUserId);

        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with keycloakUserId: " + keycloakUserId));

        if (user.getEmailVerified()) {
            log.debug("Email already verified for keycloakUserId: {}", keycloakUserId);
            return;
        }

        user.verifyEmail();
        userRepository.save(user);

        log.info("User email verified: userId={}, keycloakUserId={}", user.getId(), keycloakUserId);
    }
    @Transactional
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.suspend();
        userRepository.save(user);

        log.info("User suspended: userId={}", userId);
    }
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.activate();
        userRepository.save(user);

        log.info("User activated: userId={}", userId);
    }
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.softDelete();
        userRepository.save(user);

        log.info("User soft deleted: userId={}", userId);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    public boolean existsByKeycloakUserId(UUID keycloakUserId) {
        return userRepository.existsByKeycloakUserId(keycloakUserId);
    }
}
