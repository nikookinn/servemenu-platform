package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.*;
import com.servemenu.businessservice.application.dto.request.*;
import com.servemenu.businessservice.application.dto.response.*;
import com.servemenu.businessservice.application.service.*;
import com.servemenu.businessservice.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Settings Controller - Manages store-level and business-level settings
 * Includes: Opening Hours, WiFi, Location, Store Settings, Social Accounts
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/stores/{storeId}/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final OpeningHoursService openingHoursService;
    private final StoreSettingsService storeSettingsService;
    private final LocationDetailsService locationDetailsService;
    private final SocialAccountsService socialAccountsService;
    private final WiFiSettingsService wifiSettingsService;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    // ==================== OPENING HOURS ====================

    /**
     * Update opening hours for a store (supports multiple time slots per day)
     * POST /api/v1/businesses/{businessId}/stores/{storeId}/settings/opening-hours
     */
    @PutMapping("/opening-hours")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<List<OpeningHoursResponse>>> updateOpeningHours(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid BatchOpeningHoursRequest request,
            Authentication authentication
    ) {
        log.info("Updating opening hours for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        // Convert request to command
        List<OpeningHourCommand> commands = request.openingHours().stream()
                .map(hourRequest -> new OpeningHourCommand(
                        hourRequest.dayOfWeek(),
                        hourRequest.isOpen(),
                        hourRequest.timeSlots() != null 
                                ? hourRequest.timeSlots().stream()
                                        .map(slot -> new TimeSlotCommand(slot.openTime(), slot.closeTime()))
                                        .collect(Collectors.toList())
                                : List.of()
                ))
                .collect(Collectors.toList());

        BatchOpeningHoursCommand command = new BatchOpeningHoursCommand(commands);
        List<OpeningHoursResponse> response = openingHoursService.updateOpeningHours(storeId, command);

        return ResponseEntity.ok(ApiResponse.success(response, "Opening hours updated successfully"));
    }

    /**
     * Get opening hours for a store
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/opening-hours
     */
    @GetMapping("/opening-hours")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<List<OpeningHoursResponse>>> getOpeningHours(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        log.info("Fetching opening hours for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        List<OpeningHoursResponse> response = openingHoursService.getOpeningHours(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== STORE SETTINGS ====================

    /**
     * Update store settings (delivery methods, food display)
     * PUT /api/v1/businesses/{businessId}/stores/{storeId}/settings/store
     */
    @PutMapping("/store")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> updateStoreSettings(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid UpdateStoreSettingsRequest request,
            Authentication authentication
    ) {
        log.info("Updating store settings for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        UpdateStoreSettingsCommand command = new UpdateStoreSettingsCommand(
                request.enableDineIn(),
                request.enableTakeaway(),
                request.enablePickup(),
                request.enableDelivery(),
                request.enableGuestCheckout(),
                request.allowSpecialInstructions(),
                request.displayFullFoodName()
        );

        StoreSettingsResponse response = storeSettingsService.updateStoreSettings(storeId, command);
        return ResponseEntity.ok(ApiResponse.success(response, "Store settings updated successfully"));
    }

    /**
     * Get store settings
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/store
     */
    @GetMapping("/store")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        log.info("Fetching store settings for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        StoreSettingsResponse response = storeSettingsService.getStoreSettings(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== LOCATION DETAILS ====================

    /**
     * Update location details
     * PUT /api/v1/businesses/{businessId}/stores/{storeId}/settings/location
     */
    @PutMapping("/location")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<LocationDetailsResponse>> updateLocationDetails(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid UpdateLocationDetailsRequest request,
            Authentication authentication
    ) {
        log.info("Updating location details for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        UpdateLocationDetailsCommand command = new UpdateLocationDetailsCommand(
                request.isEnabled(),
                request.latitude(),
                request.longitude(),
                request.radiusInMeters()
        );

        LocationDetailsResponse response = locationDetailsService.updateLocationDetails(storeId, command);
        return ResponseEntity.ok(ApiResponse.success(response, "Location details updated successfully"));
    }

    /**
     * Get location details
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/location
     */
    @GetMapping("/location")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<LocationDetailsResponse>> getLocationDetails(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        log.info("Fetching location details for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        LocationDetailsResponse response = locationDetailsService.getLocationDetails(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== SOCIAL ACCOUNTS ====================

    /**
     * Update social accounts
     * PUT /api/v1/businesses/{businessId}/stores/{storeId}/settings/social
     */
    @PutMapping("/social")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<SocialAccountsResponse>> updateSocialAccounts(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid UpdateSocialAccountsRequest request,
            Authentication authentication
    ) {
        log.info("Updating social accounts for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        UpdateSocialAccountsCommand command = new UpdateSocialAccountsCommand(
                request.facebook(),
                request.twitter(),
                request.instagram(),
                request.snapchat(),
                request.pinterest(),
                request.foursquare(),
                request.tripadvisor(),
                request.zomato(),
                request.tiktok()
        );

        SocialAccountsResponse response = socialAccountsService.updateSocialAccounts(storeId, command);
        return ResponseEntity.ok(ApiResponse.success(response, "Social accounts updated successfully"));
    }

    /**
     * Get social accounts
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/social
     */
    @GetMapping("/social")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<SocialAccountsResponse>> getSocialAccounts(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        log.info("Fetching social accounts for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        SocialAccountsResponse response = socialAccountsService.getSocialAccounts(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== WIFI SETTINGS ====================

    /**
     * Create new WiFi network
     * POST /api/v1/businesses/{businessId}/stores/{storeId}/settings/wifi
     */
    @PostMapping("/wifi")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<WiFiSettingsResponse>> createWiFi(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @RequestBody @Valid CreateWiFiSettingsRequest request,
            Authentication authentication
    ) {
        log.info("Creating WiFi network for store: {}, name: {}", storeId, request.wifiName());
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        CreateWiFiSettingsCommand command = new CreateWiFiSettingsCommand(
                request.wifiName(),
                request.ssid(),
                request.password(),
                request.wifiType()
        );

        WiFiSettingsResponse response = wifiSettingsService.createWiFiSettings(storeId, command, userId);
        
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(response, "WiFi network created successfully"));
    }

    /**
     * Get all WiFi networks for a store
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/wifi
     */
    @GetMapping("/wifi")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<List<WiFiSettingsResponse>>> getAllWiFiNetworks(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        log.info("Fetching all WiFi networks for store: {}", storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        List<WiFiSettingsResponse> response = wifiSettingsService.getAllWiFiSettings(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get specific WiFi network by ID
     * GET /api/v1/businesses/{businessId}/stores/{storeId}/settings/wifi/{wifiId}
     */
    @GetMapping("/wifi/{wifiId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<WiFiSettingsResponse>> getWiFiById(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable UUID wifiId,
            Authentication authentication
    ) {
        log.info("Fetching WiFi network: wifiId={}, storeId={}", wifiId, storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        WiFiSettingsResponse response = wifiSettingsService.getWiFiSettingsById(wifiId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete WiFi network (soft delete)
     * DELETE /api/v1/businesses/{businessId}/stores/{storeId}/settings/wifi/{wifiId}
     */
    @DeleteMapping("/wifi/{wifiId}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteWiFi(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable UUID wifiId,
            Authentication authentication
    ) {
        log.info("Deleting WiFi network: wifiId={}, storeId={}", wifiId, storeId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        wifiSettingsService.deleteWiFiSettings(wifiId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "WiFi network deleted successfully"));
    }
}
