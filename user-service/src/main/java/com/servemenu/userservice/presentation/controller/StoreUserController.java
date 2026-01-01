package com.servemenu.userservice.presentation.controller;

import com.servemenu.userservice.application.dto.command.CreateStoreUserCommand;
import com.servemenu.userservice.application.dto.request.CreateStoreUserRequest;
import com.servemenu.userservice.application.dto.request.UpdateStoreUserRequest;
import com.servemenu.userservice.application.dto.response.PageResponse;
import com.servemenu.userservice.application.dto.response.StoreUserResponse;
import com.servemenu.userservice.application.dto.response.UserResponse;
import com.servemenu.userservice.application.service.StoreUserService;
import com.servemenu.userservice.application.service.UserService;
import com.servemenu.userservice.common.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store-users")
public class StoreUserController {

    private final StoreUserService storeUserService;
    private final UserService userService;

    public StoreUserController(StoreUserService storeUserService, UserService userService) {
        this.storeUserService = storeUserService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('store_admin', 'store_user')")
    public ResponseEntity<StoreUserResponse> getCurrentStoreUser() {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse currentUser = userService.findByKeycloakUserId(keycloakUserId);
        StoreUserResponse storeUser = storeUserService.findByUserId(currentUser.id());
        return ResponseEntity.ok(storeUser);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<StoreUserResponse> createStoreUser(@Valid @RequestBody CreateStoreUserRequest request) {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        UserResponse currentUser = userService.findByKeycloakUserId(keycloakUserId);

        CreateStoreUserCommand command = new CreateStoreUserCommand(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                request.businessId(),
                request.storeId(),
                request.accessLevel(),
                currentUser.id()
        );

        StoreUserResponse created = storeUserService.createStoreUser(command, currentUser.id());

        return ResponseEntity.status(HttpStatus.OK).body(created);
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<PageResponse<StoreUserResponse>> getStoreUsersByStore(
            @PathVariable UUID storeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StoreUserResponse> users = storeUserService.findByStoreId(storeId, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<StoreUserResponse> getStoreUserById(@PathVariable UUID userId) {
        StoreUserResponse user = storeUserService.findByUserId(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<StoreUserResponse> updateStoreUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStoreUserRequest request
    ) {
        StoreUserResponse updated = storeUserService.updateStoreUser(userId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<Void> deactivateStoreUser(@PathVariable UUID userId) {
        storeUserService.deactivateStoreUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/store/{storeId}/count")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<Long> countActiveStoreUsers(@PathVariable UUID storeId) {
        long count = storeUserService.countActiveByStoreId(storeId);
        return ResponseEntity.ok(count);
    }
}