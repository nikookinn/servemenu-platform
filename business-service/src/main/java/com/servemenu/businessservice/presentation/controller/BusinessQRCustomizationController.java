package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.SaveQRCustomizationCommand;
import com.servemenu.businessservice.application.dto.request.QRCustomizationRequest;
import com.servemenu.businessservice.application.dto.response.ApiResponse;
import com.servemenu.businessservice.application.dto.response.QRCustomizationResponse;
import com.servemenu.businessservice.application.mapper.QRCustomizationMapper;
import com.servemenu.businessservice.application.service.BusinessSecurityService;
import com.servemenu.businessservice.application.service.QRCustomizationService;
import com.servemenu.businessservice.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for Business-level QR Customizations (BUSINESS QR type only)
 * BUSINESS QR is shared across all stores in a business
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/qr-customization")
@RequiredArgsConstructor
@Slf4j
public class BusinessQRCustomizationController {

    private final QRCustomizationService qrCustomizationService;
    private final QRCustomizationMapper qrCustomizationMapper;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    /**
     * Get BUSINESS QR customization
     * GET /api/v1/businesses/{businessId}/qr-customization
     * Used by QR Service to fetch business-level QR customization
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'qr_service_reader')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> getBusinessQRCustomization(
            @PathVariable UUID businessId,
            Authentication authentication
    ) {
        log.info("📋 Fetching BUSINESS QR customization for businessId: {}", businessId);
        
        // Service accounts (qr_service_reader) can access without validation
        boolean isServiceAccount = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_qr_service_reader"));
        
        if (!isServiceAccount) {
            // For regular users, validate business ownership
            UUID userId = jwtUtil.extractUserId(authentication);
            securityService.validateBusinessOwnership(userId, businessId);
        }

        QRCustomizationResponse response = qrCustomizationService.getBusinessQRCustomization(businessId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update BUSINESS QR customization
     * PUT /api/v1/businesses/{businessId}/qr-customization
     */
    @PutMapping
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> updateBusinessQRCustomization(
            @PathVariable UUID businessId,
            @RequestBody @Valid QRCustomizationRequest request,
            Authentication authentication
    ) {
        log.info("✏️ Updating BUSINESS QR customization for businessId: {}", businessId);
        
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateBusinessOwnership(userId, businessId);

        SaveQRCustomizationCommand command = qrCustomizationMapper.toCommand(request);
        QRCustomizationResponse response = qrCustomizationService.saveBusinessQRCustomization(businessId, command);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Business QR customization updated successfully"));
    }
}
