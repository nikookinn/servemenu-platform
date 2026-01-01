package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.SaveQRCustomizationCommand;
import com.servemenu.businessservice.application.dto.response.QRCustomizationResponse;
import com.servemenu.businessservice.application.mapper.QRCustomizationMapper;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.enums.QRType;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.QRCustomization;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import com.servemenu.businessservice.domain.repository.QRCustomizationRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QR Customization Service - Refactored Version
 * 
 * Manages QR customizations for 3 types:
 * 1. BUSINESS - Business-level QR (business landing page)
 * 2. TABLE - Store-level QR (table menu)
 * 3. WIFI - Store-level QR (WiFi credentials)
 * 
 * Key Principles:
 * - Minimize DB calls
 * - Clear separation between business and store level
 * - No unnecessary default creation
 * - Simple and predictable method names
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class QRCustomizationService {

    private final QRCustomizationRepository qrCustomizationRepository;
    private final StoreRepository storeRepository;
    private final BusinessRepository businessRepository;
    private final QRCustomizationMapper qrCustomizationMapper;
    private final MediaEnrichmentService mediaEnrichmentService;

    // ==================== INITIALIZATION METHODS ====================
    // Called when Business/Store is created

    /**
     * Initialize BUSINESS QR customization when business is created
     * Creates default customization for business-level QR
     */
    @Transactional
    public void initializeBusinessQRCustomization(Business business) {
        log.info("Initializing BUSINESS QR customization for businessId: {}", business.getId());

        // Check if already exists (idempotency)
        if (qrCustomizationRepository.existsByBusinessIdAndQrType(business.getId(), QRType.BUSINESS)) {
            log.debug("BUSINESS QR customization already exists");
            return;
        }

        QRCustomization customization = createDefaultCustomization(QRType.BUSINESS);
        customization.setBusiness(business);
        customization.setStore(null); // Business-level, no store

        qrCustomizationRepository.save(customization);
        log.info("✅ BUSINESS QR customization initialized");
    }

    /**
     * Initialize store QR customizations when store is created
     * Creates default customizations for TABLE and WIFI types
     * Single batch insert for better performance
     */
    @Transactional
    public void initializeStoreQRCustomizations(Store store) {
        log.info("Initializing store QR customizations for storeId: {}", store.getId());

        List<QRCustomization> toSave = new ArrayList<>();

        // Check and create TABLE customization
        if (!qrCustomizationRepository.existsByStoreIdAndQrType(store.getId(), QRType.TABLE)) {
            QRCustomization tableCustomization = createDefaultCustomization(QRType.TABLE);
            tableCustomization.setStore(store);
            tableCustomization.setBusiness(null); // Store-level, no business
            toSave.add(tableCustomization);
        }

        // Check and create WIFI customization
        if (!qrCustomizationRepository.existsByStoreIdAndQrType(store.getId(), QRType.WIFI)) {
            QRCustomization wifiCustomization = createDefaultCustomization(QRType.WIFI);
            wifiCustomization.setStore(store);
            wifiCustomization.setBusiness(null); // Store-level, no business
            toSave.add(wifiCustomization);
        }

        if (!toSave.isEmpty()) {
            qrCustomizationRepository.saveAll(toSave); // Batch save
            log.info("✅ Store QR customizations initialized: {} types", toSave.size());
        }
    }

    // ==================== BUSINESS QR METHODS ====================

    /**
     * Get BUSINESS QR customization
     * Returns existing or creates response with defaults (doesn't save to DB)
     */
    public QRCustomizationResponse getBusinessQRCustomization(UUID businessId) {
        log.debug("Getting BUSINESS QR customization for businessId: {}", businessId);

        return qrCustomizationRepository.findByBusinessIdAndQrType(businessId, QRType.BUSINESS)
                .map(customization -> {
                    // Enrich with logo URL if logo exists
                    mediaEnrichmentService.enrichQRCustomization(customization);
                    return qrCustomizationMapper.toResponse(customization);
                })
                .orElseGet(() -> {
                    // Return default response without saving to DB
                    QRCustomization defaults = createDefaultCustomization(QRType.BUSINESS);
                    return qrCustomizationMapper.toResponse(defaults);
                });
    }

    /**
     * Save/Update BUSINESS QR customization
     */
    @Transactional
    public QRCustomizationResponse saveBusinessQRCustomization(
            UUID businessId,
            SaveQRCustomizationCommand command
    ) {
        log.info("Saving BUSINESS QR customization for businessId: {}", businessId);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        QRCustomization customization = qrCustomizationRepository
                .findByBusinessIdAndQrType(businessId, QRType.BUSINESS)
                .orElseGet(() -> {
                    QRCustomization newCustomization = new QRCustomization();
                    newCustomization.setBusiness(business);
                    newCustomization.setQrType(QRType.BUSINESS);
                    return newCustomization;
                });

        // Check if logo is being removed or changed
        UUID oldLogoMediaId = customization.getLogoMediaId();
        UUID newLogoMediaId = command.logoMediaId();
        boolean logoRemoved = oldLogoMediaId != null && newLogoMediaId == null;
        boolean logoChanged = oldLogoMediaId != null && newLogoMediaId != null && !oldLogoMediaId.equals(newLogoMediaId);
        
        if (logoRemoved) {
            log.info("🗑️ Logo removed from BUSINESS QR customization, old logoMediaId: {}", oldLogoMediaId);
            mediaEnrichmentService.deleteMedia(oldLogoMediaId);
        } else if (logoChanged) {
            log.info("🔄 Logo changed in BUSINESS QR customization, deleting old logo: {}", oldLogoMediaId);
            mediaEnrichmentService.deleteMedia(oldLogoMediaId);
        }

        // Update fields from command
        qrCustomizationMapper.updateEntity(customization, command);
        
        QRCustomization saved = qrCustomizationRepository.save(customization);
        log.info("✅ BUSINESS QR customization saved");

        return qrCustomizationMapper.toResponse(saved);
    }

    // ==================== STORE QR METHODS ====================

    /**
     * Get store QR customization by type (TABLE or WIFI)
     * Returns existing or creates response with defaults (doesn't save to DB)
     */
    public QRCustomizationResponse getStoreQRCustomization(UUID storeId, QRType qrType) {
        log.debug("Getting {} QR customization for storeId: {}", qrType, storeId);

        validateStoreQRType(qrType);

        return qrCustomizationRepository.findByStoreIdAndQrType(storeId, qrType)
                .map(customization -> {
                    // Enrich with logo URL if logo exists
                    mediaEnrichmentService.enrichQRCustomization(customization);
                    return qrCustomizationMapper.toResponse(customization);
                })
                .orElseGet(() -> {
                    // Return default response without saving to DB
                    QRCustomization defaults = createDefaultCustomization(qrType);
                    return qrCustomizationMapper.toResponse(defaults);
                });
    }

    /**
     * Save/Update store QR customization (TABLE or WIFI)
     */
    @Transactional
    public QRCustomizationResponse saveStoreQRCustomization(
            UUID storeId,
            QRType qrType,
            SaveQRCustomizationCommand command
    ) {
        log.info("Saving {} QR customization for storeId: {}", qrType, storeId);

        validateStoreQRType(qrType);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        QRCustomization customization = qrCustomizationRepository
                .findByStoreIdAndQrType(storeId, qrType)
                .orElseGet(() -> {
                    QRCustomization newCustomization = new QRCustomization();
                    newCustomization.setStore(store);
                    newCustomization.setQrType(qrType);
                    return newCustomization;
                });

        // Check if logo is being removed or changed
        UUID oldLogoMediaId = customization.getLogoMediaId();
        UUID newLogoMediaId = command.logoMediaId();
        boolean logoRemoved = oldLogoMediaId != null && newLogoMediaId == null;
        boolean logoChanged = oldLogoMediaId != null && newLogoMediaId != null && !oldLogoMediaId.equals(newLogoMediaId);
        
        if (logoRemoved) {
            log.info("🗑️ Logo removed from {} QR customization, old logoMediaId: {}", qrType, oldLogoMediaId);
            mediaEnrichmentService.deleteMedia(oldLogoMediaId);
        } else if (logoChanged) {
            log.info("🔄 Logo changed in {} QR customization, deleting old logo: {}", qrType, oldLogoMediaId);
            mediaEnrichmentService.deleteMedia(oldLogoMediaId);
        }

        // Update fields from command
        qrCustomizationMapper.updateEntity(customization, command);
        
        QRCustomization saved = qrCustomizationRepository.save(customization);
        log.info("✅ {} QR customization saved", qrType);

        return qrCustomizationMapper.toResponse(saved);
    }

    // ==================== INTERNAL METHODS ====================
    // Used by other services (TableService, QRRequestService, etc.)

    /**
     * Get QR customization entity for internal use
     * Returns entity if exists, null otherwise
     * Does NOT create default - caller should handle
     */
    public Optional<QRCustomization> findQRCustomization(UUID storeId, UUID businessId, QRType qrType) {
        if (qrType == QRType.BUSINESS && businessId != null) {
            return qrCustomizationRepository.findByBusinessIdAndQrType(businessId, qrType);
        } else if (storeId != null && (qrType == QRType.TABLE || qrType == QRType.WIFI)) {
            return qrCustomizationRepository.findByStoreIdAndQrType(storeId, qrType);
        }
        return Optional.empty();
    }

    /**
     * Get or create QR customization for QR generation
     * Used by QRRequestService when generating QR codes
     */
    @Transactional
    public QRCustomization ensureQRCustomizationExists(UUID storeId, UUID businessId, QRType qrType) {
        Optional<QRCustomization> existing = findQRCustomization(storeId, businessId, qrType);
        
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create and save default
        if (qrType == QRType.BUSINESS && businessId != null) {
            Business business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
            
            QRCustomization customization = createDefaultCustomization(QRType.BUSINESS);
            customization.setBusiness(business);
            return qrCustomizationRepository.save(customization);
        } else if (storeId != null) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
            
            QRCustomization customization = createDefaultCustomization(qrType);
            customization.setStore(store);
            return qrCustomizationRepository.save(customization);
        }

        throw new IllegalArgumentException("Invalid parameters for QR customization");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create default QR customization with type-specific defaults
     */
    private QRCustomization createDefaultCustomization(QRType qrType) {
        String frameText = switch (qrType) {
            case BUSINESS -> "SCAN TO VISIT";
            case TABLE -> "SCAN FOR MENU";
            case WIFI -> "SCAN FOR WIFI";
        };

        return QRCustomization.builder()
                .qrType(qrType)
                .width(800)
                .height(800)
                .margin(10)
                .backgroundColor("#FFFFFF")
                .patternColorMode("single")
                .patternColorSingle("#000000")
                .patternType("rounded")
                .eyeType("extra-rounded")
                .eyeColorEnabled(true)
                .eyeColorOuter("#132440")
                .eyeColorInner("#bf092f")
                .logoSize(30)
                .frameType("none")
                .frameText(frameText)
                .frameFont("Roboto")
                .frameTextColor("#FFFFFF")
                .frameColorMode("single")
                .frameColorSingle("#000000")
                .build();
    }

    /**
     * Validate that QR type is store-level
     */
    private void validateStoreQRType(QRType qrType) {
        if (qrType != QRType.TABLE && qrType != QRType.WIFI) {
            throw new IllegalArgumentException("Invalid store QR type: " + qrType);
        }
    }
}
