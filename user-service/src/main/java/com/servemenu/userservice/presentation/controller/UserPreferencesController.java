package com.servemenu.userservice.presentation.controller;

import com.servemenu.userservice.application.dto.request.UpdateUserPreferencesRequest;
import com.servemenu.userservice.application.dto.response.UserPreferencesResponse;
import com.servemenu.userservice.application.dto.response.UserResponse;
import com.servemenu.userservice.application.service.UserPreferencesService;
import com.servemenu.userservice.application.service.UserService;
import com.servemenu.userservice.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final UserService userService;

    public UserPreferencesController(UserPreferencesService preferencesService, UserService userService) {
        this.preferencesService = preferencesService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> getCurrentUserPreferences() {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse currentUser = userService.findByKeycloakUserId(keycloakUserId);

        UserPreferencesResponse preferences = preferencesService.findByUserId(currentUser.id());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request
    ) {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse currentUser = userService.findByKeycloakUserId(keycloakUserId);

        UserPreferencesResponse updated = preferencesService.updatePreferences(currentUser.id(), request);
        return ResponseEntity.ok(updated);
    }
}