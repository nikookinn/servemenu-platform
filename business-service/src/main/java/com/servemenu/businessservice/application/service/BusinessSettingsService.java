package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.request.UpdateBusinessSettingsRequest;
import com.servemenu.businessservice.application.dto.response.BusinessSettingsResponse;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.common.util.SlugGenerator;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.BusinessSettings;
import com.servemenu.businessservice.domain.model.NotificationSettings;
import com.servemenu.businessservice.domain.model.OrderSettings;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import com.servemenu.businessservice.domain.repository.BusinessSettingsRepository;
import com.servemenu.businessservice.domain.repository.NotificationSettingsRepository;
import com.servemenu.businessservice.domain.repository.OrderSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business Settings Service
 * Manages business-level settings (BusinessSettings, NotificationSettings, OrderSettings)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BusinessSettingsService {

    private final BusinessSettingsRepository businessSettingsRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final OrderSettingsRepository orderSettingsRepository;
    private final BusinessRepository businessRepository;
    private final MediaEnrichmentService mediaEnrichmentService;
    private final SlugGenerator slugGenerator;

    /**
     * Initialize default business settings for a new business
     */
    @Transactional
    public void initializeDefaultBusinessSettings(Business business) {
        log.info("Initializing default business settings: businessId={}", business.getId());
        
        BusinessSettings settings = BusinessSettings.builder()
                .business(business)
                .build();
        businessSettingsRepository.save(settings);
        
        log.info("✅ Default business settings initialized: businessId={}", business.getId());
    }

    /**
     * Initialize default notification settings for a new business
     */
    @Transactional
    public void initializeDefaultNotificationSettings(Business business) {
        log.info("Initializing default notification settings: businessId={}", business.getId());
        
        NotificationSettings settings = NotificationSettings.builder()
                .business(business)
                .build();
        notificationSettingsRepository.save(settings);
        
        log.info("✅ Default notification settings initialized: businessId={}", business.getId());
    }

    /**
     * Initialize default order settings for a new business
     */
    @Transactional
    public void initializeDefaultOrderSettings(Business business) {
        log.info("Initializing default order settings: businessId={}", business.getId());
        
        OrderSettings settings = OrderSettings.builder()
                .business(business)
                .build();
        orderSettingsRepository.save(settings);
        
        log.info("✅ Default order settings initialized: businessId={}", business.getId());
    }

    /**
     * Get business settings with enriched media URLs
     */
    public BusinessSettingsResponse getBusinessSettings(UUID businessId) {
        log.debug("Getting business settings: businessId={}", businessId);

        Business business = businessRepository.findByIdWithSettings(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        BusinessSettings settings = business.getBusinessSettings();
        if (settings == null) {
            throw new ResourceNotFoundException("Business settings not found for business: " + businessId);
        }

        log.info("🔍 BusinessSettings entity loaded - ID: {}, logoMediaId: {}, coverImageMediaId: {}", 
            settings.getId(), settings.getLogoMediaId(), settings.getCoverImageMediaId());

        // Enrich media URLs
        log.info("🖼️ Before enrichment - logoMediaId: {}, coverImageMediaId: {}", 
            settings.getLogoMediaId(), settings.getCoverImageMediaId());
        mediaEnrichmentService.enrichBusinessSettings(settings);
        log.info("🖼️ After enrichment - logoUrl: {}, coverImageUrl: {}", 
            settings.getLogoUrl(), settings.getCoverImageUrl());

        return mapToResponse(business, settings);
    }

    /**
     * Update business settings
     */
    @Transactional
    public BusinessSettingsResponse updateBusinessSettings(UUID businessId, UpdateBusinessSettingsRequest request) {
        log.info("Updating business settings: businessId={}", businessId);

        Business business = businessRepository.findByIdWithSettings(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

        BusinessSettings settings = business.getBusinessSettings();
        if (settings == null) {
            throw new ResourceNotFoundException("Business settings not found for business: " + businessId);
        }

        // Update Business entity fields
        boolean businessNameChanged = !business.getBusinessName().equals(request.businessName());
        business.setBusinessName(request.businessName());
        business.setCurrency(request.currency());
        business.setSupportedLanguages(request.supportedLanguages());
        business.setDefaultLanguage(request.defaultLanguage());

        // Regenerate slug if business name changed
        if (businessNameChanged) {
            String newSlug = slugGenerator.generate(request.businessName(), business.getBusinessOwnerId());
            business.setSlug(newSlug);
            log.info("Business name changed, regenerated slug: oldName={}, newName={}, newSlug={}",
                    business.getBusinessName(), request.businessName(), newSlug);
        }

        // Update BusinessSettings entity fields
        log.info("📥 Received mediaIds - logoMediaId: {}, coverImageMediaId: {}", 
            request.logoMediaId(), request.coverImageMediaId());
        
        settings.setLogoMediaId(request.logoMediaId());
        settings.setCoverImageMediaId(request.coverImageMediaId());
        settings.setAddress(request.address());
        settings.setEmail(request.email());
        settings.setPhoneNumber(request.phoneNumber());
        settings.setCountryCode(request.countryCode());

        log.info("💾 Saving BusinessSettings - logoMediaId: {}, coverImageMediaId: {}", 
            settings.getLogoMediaId(), settings.getCoverImageMediaId());

        // Save changes
        businessRepository.save(business);
        businessSettingsRepository.save(settings);
        
        log.info("✅ BusinessSettings saved to database - ID: {}", settings.getId());

        // Enrich media URLs
        mediaEnrichmentService.enrichBusinessSettings(settings);

        log.info("✅ Business settings updated successfully: businessId={}", businessId);
        return mapToResponse(business, settings);
    }

    /**
     * Map Business and BusinessSettings to response DTO
     */
    private BusinessSettingsResponse mapToResponse(Business business, BusinessSettings settings) {
        return new BusinessSettingsResponse(
                settings.getId(),
                business.getId(),
                business.getBusinessName(),
                business.getSlug(),
                business.getCurrency(),
                business.getSupportedLanguages(),
                business.getDefaultLanguage(),
                settings.getLogoMediaId(),
                settings.getLogoUrl(),
                settings.getCoverImageMediaId(),
                settings.getCoverImageUrl(),
                settings.getAddress(),
                settings.getEmail(),
                settings.getPhoneNumber(),
                settings.getCountryCode(),
                settings.getEnableDefaultFoodImage(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}
