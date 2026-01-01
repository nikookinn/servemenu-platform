package com.servemenu.userservice.presentation.controller;

import com.servemenu.userservice.application.dto.response.BusinessOwnerResponse;
import com.servemenu.userservice.application.service.BusinessOwnerService;
import com.servemenu.userservice.common.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-owners")
public class BusinessOwnerController {

    private final BusinessOwnerService businessOwnerService;

    public BusinessOwnerController(BusinessOwnerService businessOwnerService) {
        this.businessOwnerService = businessOwnerService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<BusinessOwnerResponse> getCurrentBusinessOwner() {
        UUID keycloakUserId = SecurityUtils.getCurrentUserKeycloakId();
        BusinessOwnerResponse owner = businessOwnerService.findByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(owner);
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('business_owner')")
    public ResponseEntity<BusinessOwnerResponse> getBusinessOwnerByUserId(@PathVariable UUID userId) {
        BusinessOwnerResponse owner = businessOwnerService.findByUserId(userId);
        return ResponseEntity.ok(owner);
    }
}