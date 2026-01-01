package com.servemenu.userservice.application.service;

import com.servemenu.userservice.application.dto.request.UpdateUserPreferencesRequest;
import com.servemenu.userservice.application.dto.response.UserPreferencesResponse;
import com.servemenu.userservice.application.mapper.UserMapper;
import com.servemenu.userservice.common.exception.ResourceNotFoundException;
import com.servemenu.userservice.domain.model.UserPreferences;
import com.servemenu.userservice.domain.repository.UserPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserPreferencesService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository, UserMapper userMapper) {
        this.preferencesRepository = preferencesRepository;
        this.userMapper = userMapper;
    }

    public UserPreferencesResponse findByUserId(UUID userId) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User preferences not found"));

        return userMapper.toUserPreferencesResponse(preferences);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UUID userId, UpdateUserPreferencesRequest request) {
        log.debug("Updating user preferences: userId={}", userId);
        
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User preferences not found"));

        // Update preferences using domain method (validates inputs)
        preferences.updatePreferences(
            request.dashboardLanguage(),
            request.timezone(),
            request.theme()
        );

        UserPreferences updated = preferencesRepository.save(preferences);

        log.info("User preferences updated: userId={}, language={}, timezone={}, theme={}", 
            userId, updated.getDashboardLanguage(), updated.getTimezone(), updated.getTheme());

        return userMapper.toUserPreferencesResponse(updated);
    }
}