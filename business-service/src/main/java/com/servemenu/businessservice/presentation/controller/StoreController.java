package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.CreateStoreCommand;
import com.servemenu.businessservice.application.dto.command.UpdateStoreCommand;
import com.servemenu.businessservice.application.dto.request.AssignMenuRequest;
import com.servemenu.businessservice.application.dto.request.CreateStoreRequest;
import com.servemenu.businessservice.application.dto.request.UpdateStoreRequest;
import com.servemenu.businessservice.application.dto.response.ApiResponse;
import com.servemenu.businessservice.application.dto.response.PageResponse;
import com.servemenu.businessservice.application.dto.response.StoreDetailResponse;
import com.servemenu.businessservice.application.dto.response.StoreListResponse;
import com.servemenu.businessservice.application.dto.response.StoreResponse;
import com.servemenu.businessservice.application.service.BusinessSecurityService;
import com.servemenu.businessservice.application.service.BusinessService;
import com.servemenu.businessservice.application.service.StoreService;
import com.servemenu.businessservice.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final BusinessService businessService;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @PathVariable UUID businessId,
            @RequestBody @Valid CreateStoreRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        if (!businessService.canCreateMoreStores(businessId)) {
            throw new com.servemenu.businessservice.common.exception.StoreLimitExceededException(
                    "Upgrade your subscription to create more stores"
            );
        }

        CreateStoreCommand command = new CreateStoreCommand(
                request.storeName(),
                request.address(),
                request.phoneNumber(),
                request.countryCode()
        );

        StoreResponse response = storeService.createStore(businessId, command, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Store created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<PageResponse<StoreListResponse>>> getStores(
            @PathVariable UUID businessId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        
        // Only business owner can list stores - validate ownership
        securityService.validateBusinessOwnership(userId, businessId);

        PageResponse<StoreListResponse> response = storeService.getStoresByBusinessId(businessId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<StoreDetailResponse>> getStoreById(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        StoreDetailResponse response = storeService.getStoreById(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid UpdateStoreRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        UpdateStoreCommand command = new UpdateStoreCommand(
                request.storeName(),
                request.address(),
                request.phoneNumber(),
                request.countryCode()
        );

        StoreResponse response = storeService.updateStore(storeId, command, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Store updated successfully"));
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteStore(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        storeService.deleteStore(storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Store deleted successfully"));
    }

    @PatchMapping("/{storeId}/menu")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<Void>> assignMenu(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid AssignMenuRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        storeService.assignMenuToStore(storeId, request.menuId());
        return ResponseEntity.ok(ApiResponse.success(null, "Menu assigned successfully"));
    }
}
