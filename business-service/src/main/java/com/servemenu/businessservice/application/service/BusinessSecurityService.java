package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.common.exception.UnauthorizedException;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessSecurityService {

    private final BusinessRepository businessRepository;
    private final StoreRepository storeRepository;

    /**
     * Validate business ownership
     */
    public void validateBusinessOwnership(UUID userId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new UnauthorizedException("Business not found"));

        if (!business.getBusinessOwnerId().equals(userId)) {
            log.warn("Unauthorized business access attempt: userId={}, businessId={}", userId, businessId);
            throw new UnauthorizedException("You don't have permission to access this business");
        }
    }

    /**
     * Validate store access (business owner or store user)
     */
    public void validateStoreAccess(UUID userId, UUID storeId, Authentication authentication) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new UnauthorizedException("Store not found"));

        UUID businessId = store.getBusiness().getId();

        // Check if user is business owner
        if (isBusinessOwner(userId, businessId)) {
            return;
        }

        // Check if user has store_admin or store_user role
        boolean isStoreUser = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_store_admin")
                                || auth.getAuthority().equals("ROLE_store_user"));

        if (!isStoreUser) {
            log.warn("Unauthorized store access: userId={}, storeId={}", userId, storeId);
            throw new UnauthorizedException("You don't have permission to access this store");
        }

        // For store users, access is granted if they have the role
        // Store-specific validation is done at the service layer
    }

    /**
     * Validate that store belongs to the specified business
     */
    public void validateStoreBusinessRelationship(UUID businessId, UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new UnauthorizedException("Store not found"));

        if (!store.getBusiness().getId().equals(businessId)) {
            log.warn("Store does not belong to business: businessId={}, storeId={}, actualBusinessId={}", 
                    businessId, storeId, store.getBusiness().getId());
            throw new UnauthorizedException("Store does not belong to this business");
        }
    }

    /**
     * Check if user is business owner
     */
    public boolean isBusinessOwner(UUID userId, UUID businessId) {
        return businessRepository.findById(businessId)
                .map(business -> business.getBusinessOwnerId().equals(userId))
                .orElse(false);
    }

}
