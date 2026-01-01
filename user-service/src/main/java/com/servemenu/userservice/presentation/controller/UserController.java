package com.servemenu.userservice.presentation.controller;

import com.servemenu.userservice.application.dto.request.UpdateProfileRequest;
import com.servemenu.userservice.application.dto.response.UserResponse;
import com.servemenu.userservice.application.service.UserService;
import com.servemenu.userservice.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse user = userService.findByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse currentUser = userService.findByKeycloakUserId(keycloakUserId);

        UserResponse updated = userService.updateProfile(
                currentUser.id(),
                request.firstName(),
                request.lastName()
        );

        return ResponseEntity.ok(updated);
    }

}