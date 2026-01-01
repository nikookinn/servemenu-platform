package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.SetupBusinessDetailsCommand;
import com.servemenu.businessservice.application.dto.command.UpdateBusinessCommand;
import com.servemenu.businessservice.application.dto.request.SetupBusinessDetailsRequest;
import com.servemenu.businessservice.application.dto.request.UpdateBusinessRequest;
import com.servemenu.businessservice.application.dto.request.UpdateBusinessSettingsRequest;
import com.servemenu.businessservice.application.dto.response.ApiResponse;
import com.servemenu.businessservice.application.dto.response.BusinessDetailResponse;
import com.servemenu.businessservice.application.dto.response.BusinessQRResponse;
import com.servemenu.businessservice.application.dto.response.BusinessResponse;
import com.servemenu.businessservice.application.dto.response.BusinessSettingsResponse;
import com.servemenu.businessservice.application.service.BusinessSecurityService;
import com.servemenu.businessservice.application.service.BusinessService;
import com.servemenu.businessservice.application.service.BusinessSettingsService;
import com.servemenu.businessservice.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessSettingsService businessSettingsService;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    @PostMapping("/setup")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessResponse>> setupBusiness(
            @RequestBody @Valid SetupBusinessDetailsRequest request,
            Authentication authentication
    ) {
        UUID businessOwnerId = jwtUtil.extractUserId(authentication);

        SetupBusinessDetailsCommand command = new SetupBusinessDetailsCommand(
                businessOwnerId,
                request.businessName(),
                request.businessType(),
                request.currency(),
                request.supportedLanguages()
        );

        BusinessResponse response = businessService.setupBusinessDetails(businessOwnerId, command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Business setup completed successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessResponse>> getMyBusiness(Authentication authentication) {
        UUID businessOwnerId = jwtUtil.extractUserId(authentication);
        BusinessResponse response = businessService.getBusinessByOwnerId(businessOwnerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{businessId}")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessDetailResponse>> getBusinessById(
            @PathVariable UUID businessId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        BusinessDetailResponse response = businessService.getBusinessById(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{businessId}")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessResponse>> updateBusiness(
            @PathVariable UUID businessId,
            @RequestBody @Valid UpdateBusinessRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        UpdateBusinessCommand command = new UpdateBusinessCommand(
                request.businessName(),
                request.supportedLanguages()
        );

        BusinessResponse response = businessService.updateBusiness(businessId, command, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Business updated successfully"));
    }

    /**
     * Get business QR code with lazy loading
     * Returns QR image URL and customer app URL
     */
    @GetMapping("/{businessId}/qr")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessQRResponse>> getBusinessQR(
            @PathVariable UUID businessId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        BusinessQRResponse response = businessService.getBusinessQR(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get business settings (logo, cover, address, email, phone, languages, currency)
     * GET /api/v1/businesses/{businessId}/settings
     */
    @GetMapping("/{businessId}/settings")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessSettingsResponse>> getBusinessSettings(
            @PathVariable UUID businessId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        BusinessSettingsResponse response = businessSettingsService.getBusinessSettings(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update business settings (logo, cover, business name, address, email, phone, languages, currency)
     * PUT /api/v1/businesses/{businessId}/settings
     */
    @PutMapping("/{businessId}/settings")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<BusinessSettingsResponse>> updateBusinessSettings(
            @PathVariable UUID businessId,
            @RequestBody @Valid UpdateBusinessSettingsRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        BusinessSettingsResponse response = businessSettingsService.updateBusinessSettings(businessId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Business settings updated successfully"));
    }
}

