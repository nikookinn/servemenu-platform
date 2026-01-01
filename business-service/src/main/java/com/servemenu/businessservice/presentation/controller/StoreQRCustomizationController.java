package com.servemenu.businessservice.presentation.controller;

import com.servemenu.businessservice.application.dto.command.SaveQRCustomizationCommand;
import com.servemenu.businessservice.application.dto.request.QRCustomizationRequest;
import com.servemenu.businessservice.application.dto.response.ApiResponse;
import com.servemenu.businessservice.application.dto.response.QRCustomizationResponse;
import com.servemenu.businessservice.application.mapper.QRCustomizationMapper;
import com.servemenu.businessservice.application.service.BusinessSecurityService;
import com.servemenu.businessservice.application.service.QRCustomizationService;
import com.servemenu.businessservice.common.util.JwtUtil;
import com.servemenu.businessservice.domain.model.QRCustomization;
import com.servemenu.businessservice.domain.enums.QRType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for Store-level QR Customizations (TABLE and WIFI QR types)
 * Each store has its own TABLE and WIFI QR customizations
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/stores/{storeId}/qr-customizations")
@RequiredArgsConstructor
public class StoreQRCustomizationController {

    private final QRCustomizationService qrCustomizationService;
    private final QRCustomizationMapper qrCustomizationMapper;
    private final BusinessSecurityService securityService;
    private final JwtUtil jwtUtil;

    /**
     * Get all QR customizations for a store
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user')")
    public ResponseEntity<ApiResponse<List<QRCustomizationResponse>>> getAllQRCustomizations(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        // Get both TABLE and WIFI customizations
        QRCustomizationResponse tableQR = qrCustomizationService.getStoreQRCustomization(storeId, QRType.TABLE);
        QRCustomizationResponse wifiQR = qrCustomizationService.getStoreQRCustomization(storeId, QRType.WIFI);
        
        List<QRCustomizationResponse> response = List.of(tableQR, wifiQR);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get QR customization by type (returns existing or null if not found)
     */
    @GetMapping("/{qrType}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin', 'store_user', 'qr_service_reader')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> getQRCustomizationByType(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable QRType qrType,
            Authentication authentication
    ) {
        // Skip user validation for service accounts (they have qr_service_reader role)
        boolean isServiceAccount = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_qr_service_reader"));
        
        if (!isServiceAccount) {
            UUID userId = jwtUtil.extractUserId(authentication);
            securityService.validateStoreBusinessRelationship(businessId, storeId);
            securityService.validateStoreAccess(userId, storeId, authentication);
        }

        QRCustomizationResponse response = qrCustomizationService.getStoreQRCustomization(storeId, qrType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get or create default QR customization by type
     * Used by frontend to get default values before customization (e.g., WiFi QR flow)
     * If customization doesn't exist, creates and saves default values
     */
    @GetMapping("/{qrType}/default")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> getOrCreateDefaultQRCustomization(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable QRType qrType,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        // Get or create default customization (saves to DB if not exists)
        QRCustomization customization = qrCustomizationService.ensureQRCustomizationExists(storeId, null, qrType);
        QRCustomizationResponse response = qrCustomizationMapper.toResponse(customization);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Default QR customization retrieved"));
    }

    /**
     * Create/Update QR customization by type
     */
    @PostMapping("/{qrType}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> saveQRCustomization(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable QRType qrType,
            @RequestBody @Valid QRCustomizationRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        SaveQRCustomizationCommand command = qrCustomizationMapper.toCommand(request);
        QRCustomizationResponse response = qrCustomizationService.saveStoreQRCustomization(storeId, qrType, command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "QR customization saved successfully"));
    }

    /**
     * Update QR customization by type
     */
    @PutMapping("/{qrType}")
    @PreAuthorize("hasAnyRole('business_owner', 'store_admin')")
    public ResponseEntity<ApiResponse<QRCustomizationResponse>> updateQRCustomization(
            @PathVariable UUID businessId,
            @PathVariable UUID storeId,
            @PathVariable QRType qrType,
            @RequestBody @Valid QRCustomizationRequest request,
            Authentication authentication
    ) {
        UUID userId = jwtUtil.extractUserId(authentication);
        securityService.validateStoreBusinessRelationship(businessId, storeId);
        securityService.validateStoreAccess(userId, storeId, authentication);

        SaveQRCustomizationCommand command = qrCustomizationMapper.toCommand(request);
        QRCustomizationResponse response = qrCustomizationService.saveStoreQRCustomization(storeId, qrType, command);
        
        return ResponseEntity.ok(ApiResponse.success(response, "QR customization updated successfully"));
    }
}
