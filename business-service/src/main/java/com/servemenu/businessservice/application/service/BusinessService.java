package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.*;
import com.servemenu.businessservice.application.dto.response.*;
import com.servemenu.businessservice.application.mapper.*;
import com.servemenu.businessservice.common.exception.*;
import com.servemenu.businessservice.common.util.SlugGenerator;
import com.servemenu.businessservice.domain.enums.*;
import com.servemenu.businessservice.domain.event.*;
import com.servemenu.businessservice.domain.model.*;
import com.servemenu.businessservice.domain.repository.*;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "business")
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessAuditLogRepository auditLogRepository;
    private final StoreService storeService;
    private final BusinessSettingsService businessSettingsService;
    private final DomainEventPublisher eventPublisher;
    private final QRRequestService qrRequestService;
    private final QRCustomizationService qrCustomizationService;
    private final SlugGenerator slugGenerator;
    private final BusinessMapper businessMapper;
    private final BusinessQRMapper businessQRMapper;

    /**
     * Setup business details during onboarding
     * Auto-creates DEFAULT store with same name
     */
    @Transactional
    @CacheEvict(allEntries = true)
    public BusinessResponse setupBusinessDetails(
            UUID businessOwnerId,
            SetupBusinessDetailsCommand command
    ) {
        log.info("Setting up business details: ownerId={}, businessName={}",
                businessOwnerId, command.businessName());

        Business business = businessRepository
                .findByBusinessOwnerIdAndStatus(businessOwnerId, BusinessStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Draft business not found. Please contact support."
                ));

        // Validate language
        if (command.supportedLanguages() == null || command.supportedLanguages().isEmpty()) {
            throw new InvalidRequestException("At least one language is required");
        }

        business.setBusinessName(command.businessName());
        business.setBusinessType(command.businessType());
        business.setCurrency(command.currency());
        business.setSupportedLanguages(command.supportedLanguages());
        business.setDefaultLanguage(command.supportedLanguages().get(0));
        business.setSlug(slugGenerator.generate(command.businessName(), businessOwnerId));
        business.setStatus(BusinessStatus.ACTIVE);

        Business savedBusiness = businessRepository.save(business);

        // Initialize default settings
        initializeDefaultSettings(savedBusiness);

        // Initialize default BUSINESS QR customization
        qrCustomizationService.initializeBusinessQRCustomization(savedBusiness);

        // Auto-create DEFAULT store (same name as business)
        Store defaultStore = storeService.createDefaultStoreInternal(
                savedBusiness,
                command.businessName()
        );

        // Mark onboarding as completed
        savedBusiness.completeOnboarding();
        businessRepository.save(savedBusiness);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.businessCreated(
                        savedBusiness.getId(),
                        businessOwnerId,
                        savedBusiness.getBusinessName()
                )
        );

        // Publish events
        eventPublisher.publish(new BusinessCreatedEvent(
                savedBusiness.getId(),
                savedBusiness.getBusinessOwnerId(),
                savedBusiness.getKeycloakUserId(),
                savedBusiness.getBusinessName(),
                savedBusiness.getSlug(),
                savedBusiness.getCurrency(),
                savedBusiness.getSupportedLanguages()
        ));

        eventPublisher.publish(new StoreCreatedEvent(
                defaultStore.getId(),
                savedBusiness.getId(),
                defaultStore.getStoreName(),
                true
        ));

        eventPublisher.publish(new BusinessDetailsCompletedEvent(
                savedBusiness.getId(),
                savedBusiness.getBusinessOwnerId(),
                savedBusiness.getKeycloakUserId(),
                defaultStore.getId()
        ));

        // Request BUSINESS QR code generation (business-level, not store-specific)
        qrRequestService.publishBusinessQRRequest(savedBusiness);

        log.info("✅ Business setup completed: businessId={}, defaultStoreId={}",
                savedBusiness.getId(), defaultStore.getId());

        return businessMapper.toResponse(savedBusiness);
    }

    /**
     * Get business by owner ID
     */
    // @Cacheable(key = "#businessOwnerId") // Temporarily disabled due to Redis serialization issues
    public BusinessResponse getBusinessByOwnerId(UUID businessOwnerId) {
        log.debug("Fetching business by ownerId: {}", businessOwnerId);

        Business business = businessRepository
                .findByBusinessOwnerIdAndStatusNot(businessOwnerId, BusinessStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return businessMapper.toResponse(business);
    }

    /**
     * Get business by ID (with details)
     */
    // @Cacheable(key = "#businessId") // Temporarily disabled due to Redis serialization issues
    public BusinessDetailResponse getBusinessById(UUID businessId) {
        log.debug("Fetching business by id: {}", businessId);

        Business business = businessRepository
                .findByIdWithSettings(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return businessMapper.toDetailResponse(business);
    }

    /**
     * Get business by slug (public access)
     */
    // @Cacheable(key = "'slug:' + #slug") // Temporarily disabled due to Redis serialization issues
    public BusinessResponse getBusinessBySlug(String slug) {
        log.debug("Fetching business by slug: {}", slug);

        Business business = businessRepository
                .findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new ResourceNotFoundException("Business not available");
        }

        return businessMapper.toResponse(business);
    }

    /**
     * Update business
     */
    @Transactional
    @CacheEvict(allEntries = true)
    public BusinessResponse updateBusiness(
            UUID businessId,
            UpdateBusinessCommand command,
            UUID performedBy
    ) {
        log.info("Updating business: businessId={}", businessId);

        Business business = findBusinessById(businessId);

        boolean changed = false;

        if (command.businessName() != null && !command.businessName().equals(business.getBusinessName())) {
            business.setBusinessName(command.businessName());
            // Regenerate slug
            business.setSlug(slugGenerator.generate(
                    command.businessName(),
                    business.getBusinessOwnerId()
            ));
            changed = true;
        }

        if (command.supportedLanguages() != null) {
            business.setSupportedLanguages(command.supportedLanguages());
            changed = true;
        }

        if (!changed) {
            return businessMapper.toResponse(business);
        }

        Business updated = businessRepository.save(business);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.storeUpdated(
                        updated.getId(),
                        null,
                        performedBy,
                        java.util.Map.of(
                                "businessName", updated.getBusinessName(),
                                "supportedLanguages", updated.getSupportedLanguages()
                        )
                )
        );

        // Publish event
        eventPublisher.publish(new BusinessUpdatedEvent(
                updated.getId(),
                updated.getBusinessOwnerId(),
                updated.getBusinessName(),
                List.of("businessName", "supportedLanguages")
        ));

        return businessMapper.toResponse(updated);
    }

    /**
     * Check if business can create more stores
     */
    public boolean canCreateMoreStores(UUID businessId) {
        Business business = findBusinessById(businessId);
        return business.canCreateMoreStores();
    }

    /**
     * Validate business ownership
     */
    public void validateOwnership(UUID businessOwnerId, UUID businessId) {
        Business business = findBusinessById(businessId);
        if (!business.getBusinessOwnerId().equals(businessOwnerId)) {
            throw new UnauthorizedException("You don't have permission to access this business");
        }
    }

    /**
     * Find business entity by ID (for internal use / gRPC)
     */
    public Business findBusinessById(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    /**
     * Find business entity by owner ID (for internal use / gRPC)
     */
    public Business findBusinessByOwnerId(UUID businessOwnerId) {
        return businessRepository.findByBusinessOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for owner"));
    }

    /**
     * Update business with QR media ID (called by MediaEventConsumer)
     */
    @Transactional
    @CacheEvict(allEntries = true)
    public void updateBusinessQRMediaId(UUID businessId, UUID mediaId) {
        log.info("Updating business QR media ID: businessId={}, mediaId={}", businessId, mediaId);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        business.setBusinessQrMediaId(mediaId);
        businessRepository.save(business);

        log.info("✅ Business QR media ID updated: businessId={}, mediaId={}", businessId, mediaId);
    }

    /**
     * Get business QR code with lazy loading
     * Fetches QR URLs from Media Service via gRPC (same as TableMapper pattern)
     */
    public BusinessQRResponse getBusinessQR(UUID businessId) {
        log.debug("Fetching business QR: businessId={}", businessId);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Use mapper to enrich with media URLs from Media Service (same as tables)
        return businessQRMapper.toQRResponse(business);
    }

    // Private helper methods

    private void initializeDefaultSettings(Business business) {
        // Initialize all business-level settings
        // Pass Business entity directly to avoid unnecessary DB queries
        businessSettingsService.initializeDefaultBusinessSettings(business);
        businessSettingsService.initializeDefaultNotificationSettings(business);
        businessSettingsService.initializeDefaultOrderSettings(business);
        
        // Initialize BUSINESS QR Customization (business-level)
        // Pass Business entity directly to avoid unnecessary DB query
        qrCustomizationService.initializeBusinessQRCustomization(business);

        log.info("✅ Default settings initialized for business: {}", business.getId());
    }
}