package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.CreateStoreCommand;
import com.servemenu.businessservice.application.dto.command.UpdateStoreCommand;
import com.servemenu.businessservice.application.dto.response.PageResponse;
import com.servemenu.businessservice.application.dto.response.StoreDetailResponse;
import com.servemenu.businessservice.application.dto.response.StoreListResponse;
import com.servemenu.businessservice.application.dto.response.StoreResponse;
import com.servemenu.businessservice.application.mapper.StoreMapper;
import com.servemenu.businessservice.common.exception.DuplicateResourceException;
import com.servemenu.businessservice.common.exception.StoreLimitExceededException;
import com.servemenu.businessservice.domain.enums.StoreStatus;
import com.servemenu.businessservice.domain.event.MenuAssignedEvent;
import com.servemenu.businessservice.domain.event.StoreCreatedEvent;
import com.servemenu.businessservice.domain.event.StoreUpdatedEvent;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.BusinessAuditLog;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.model.StoreSettings;
import com.servemenu.businessservice.domain.repository.BusinessAuditLogRepository;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import com.servemenu.businessservice.domain.repository.StoreSettingsRepository;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "store")
public class StoreService {

    private final StoreRepository storeRepository;
    private final BusinessRepository businessRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final OpeningHoursService openingHoursService;
    private final SocialAccountsService socialAccountsService;
    private final LocationDetailsService locationDetailsService;
    private final BusinessAuditLogRepository auditLogRepository;
    private final DomainEventPublisher eventPublisher;
    private final StoreMapper storeMapper;
    private final QRCustomizationService qrCustomizationService;
    private final StoreCodeGenerator storeCodeGenerator;

    /**
     * Create DEFAULT store (internal - called by BusinessService)
     */
    @Transactional
    public Store createDefaultStoreInternal(Business business, String storeName) {
        log.info("Creating default store: businessId={}, storeName={}",
                business.getId(), storeName);

        // Generate unique 4-digit store code within this business
        String storeCode = storeCodeGenerator.generateStoreCode(business.getId());

        Store store = Store.builder()
                .business(business)
                .storeName(storeName)
                .storeCode(storeCode)
                .isDefault(true)
                .status(StoreStatus.ACTIVE)
                .timezone("UTC")
                .build();

        Store saved = storeRepository.save(store);
        
        log.info("Default store created with code: storeId={}, storeCode={}", 
                saved.getId(), storeCode);

        // Initialize default store settings
        initializeDefaultStoreSettings(saved);

        log.info("Default store created: storeId={}", saved.getId());
        return saved;
    }

    /**
     * Create additional store (by user request)
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public StoreResponse createStore(
            UUID businessId,
            CreateStoreCommand command,
            UUID performedBy
    ) {
        log.info("Creating additional store: businessId={}, storeName={}",
                businessId, command.storeName());

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Check store limit
        if (!business.canCreateMoreStores()) {
            throw new StoreLimitExceededException(
                    "Store limit reached for current subscription plan. " +
                            "Current plan: " + business.getSubscriptionPlan().getDisplayName()
            );
        }

        // Check duplicate name
        if (storeRepository.existsByBusinessIdAndStoreName(businessId, command.storeName())) {
            throw new DuplicateResourceException("Store with this name already exists");
        }

        // Generate unique 4-digit store code within this business
        String storeCode = storeCodeGenerator.generateStoreCode(businessId);

        Store store = Store.builder()
                .business(business)
                .storeName(command.storeName())
                .storeCode(storeCode)
                .address(command.address())
                .phoneNumber(command.phoneNumber())
                .countryCode(command.countryCode())
                .isDefault(false)
                .status(StoreStatus.ACTIVE)
                .timezone("UTC")
                .build();

        Store saved = storeRepository.save(store);
        
        log.info("Store created with code: storeId={}, storeCode={}", 
                saved.getId(), storeCode);

        // Initialize settings
        initializeDefaultStoreSettings(saved);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.storeCreated(
                        businessId,
                        saved.getId(),
                        performedBy,
                        saved.getStoreName()
                )
        );

        // Publish event
        eventPublisher.publish(new StoreCreatedEvent(
                saved.getId(),
                businessId,
                saved.getStoreName(),
                false
        ));

        log.info("Additional store created: storeId={}", saved.getId());
        return storeMapper.toResponse(saved);
    }

    /**
     * Get all stores for a business
     * Uses PageResponse wrapper for proper Redis cache serialization
     */
    // @Cacheable(value = CacheNames.STORE, key = "'business:' + #businessId + ':page:' + #pageable.pageNumber")
    public PageResponse<StoreListResponse> getStoresByBusinessId(
            UUID businessId,
            Pageable pageable
    ) {
        log.debug("Fetching stores: businessId={}, page={}", businessId, pageable.getPageNumber());

        Page<Store> stores = storeRepository
                .findByBusinessIdAndStatusNot(businessId, StoreStatus.DELETED, pageable);

        Page<StoreListResponse> mapped = stores.map(storeMapper::toListResponse);
        return PageResponse.of(mapped);
    }

    /**
     * Get single store by storeId for store users
     * Returns a page with single store for consistency with pagination
     */
    public PageResponse<StoreListResponse> getStoresByStoreId(
            UUID businessId,
            UUID storeId,
            Pageable pageable
    ) {
        log.debug("Fetching single store for store user: businessId={}, storeId={}", businessId, storeId);

        Store store = storeRepository
                .findById(storeId)
                .filter(s -> s.getBusiness().getId().equals(businessId))
                .filter(s -> s.getStatus() != StoreStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found or access denied"));

        StoreListResponse response = storeMapper.toListResponse(store);
        
        // Return as a single-item page
        Page<StoreListResponse> page = new PageImpl<>(List.of(response), pageable, 1);
        return PageResponse.of(page);
    }

    /**
     * Get store by ID
     */
    // @Cacheable(value = CacheNames.STORE, key = "#storeId")
    public StoreDetailResponse getStoreById(UUID storeId) {
        log.debug("Fetching store: storeId={}", storeId);

        Store store = storeRepository
                .findByIdWithSettings(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        return storeMapper.toDetailResponse(store);
    }

    /**
     * Update store
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public StoreResponse updateStore(
            UUID storeId,
            UpdateStoreCommand command,
            UUID performedBy
    ) {
        log.info("Updating store: storeId={}", storeId);

        Store store = findStoreById(storeId);

        if (command.storeName() != null) {
            store.setStoreName(command.storeName());
        }
        if (command.address() != null) {
            store.setAddress(command.address());
        }
        if (command.phoneNumber() != null) {
            store.updateContactInfo(
                    command.phoneNumber(),
                    command.countryCode()
            );
        }

        Store updated = storeRepository.save(store);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.storeUpdated(
                        store.getBusiness().getId(),
                        storeId,
                        performedBy,
                        java.util.Map.of("storeName", updated.getStoreName())
                )
        );

        // Publish event
        eventPublisher.publish(new StoreUpdatedEvent(
                updated.getId(),
                updated.getBusiness().getId(),
                updated.getStoreName(),
                List.of("storeName", "description", "address")
        ));

        return storeMapper.toResponse(updated);
    }

    /**
     * Delete store (soft delete by setting status to DELETED)
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public void deleteStore(UUID storeId, UUID performedBy) {
        log.info("Soft deleting store and related data: storeId={}, performedBy={}", storeId, performedBy);

        Store store = findStoreById(storeId);
        UUID businessId = store.getBusiness().getId();
        
        // Collect table IDs and QR media IDs for batch processing
        List<UUID> tableIds = new java.util.ArrayList<>();
        List<UUID> qrCodeMediaIds = new java.util.ArrayList<>();
        
        // SOFT DELETE: Tables (audit trail)
        store.getTables().forEach(table -> {
            if (!table.isDeleted()) {
                table.softDelete(performedBy);
                tableIds.add(table.getId());
                if (table.getQrCodeMediaId() != null) {
                    qrCodeMediaIds.add(table.getQrCodeMediaId());
                }
            }
        });
        
        log.info("Soft deleted {} tables with {} QR codes for store: {}", 
                tableIds.size(), qrCodeMediaIds.size(), storeId);
        
        // HARD DELETE: Configuration entities (non-critical, re-creatable data)
        // Professional approach: Delete configuration but keep audit trail for main entities
        if (store.getSettings() != null) {
            store.setSettings(null);  // Remove reference, CASCADE will delete
        }
        if (store.getSocialAccounts() != null) {
            store.setSocialAccounts(null);
        }
        if (store.getLocationDetails() != null) {
            store.setLocationDetails(null);
        }
        store.getWifiSettings().clear();  // Clear collection, orphanRemoval will delete
        store.getOpeningHours().clear();
        store.getQrCustomizations().clear();
        
        // SOFT DELETE: Store (main entity - audit trail required)
        store.setStatus(StoreStatus.DELETED);
        store.setDeletedAt(Instant.now());
        storeRepository.save(store);  // Save triggers CASCADE delete for cleared entities
        
        // Publish SINGLE StoreDeletedEvent with batch data
        // Media Service will cleanup all QR codes in one operation
        // User Service will cleanup all store users in one operation
        eventPublisher.publish(new com.servemenu.businessservice.domain.event.StoreDeletedEvent(
                storeId,
                businessId,
                tableIds,
                qrCodeMediaIds
        ));

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.storeDeleted(
                        businessId,
                        storeId,
                        performedBy,
                        java.util.Map.of(
                                "storeName", store.getStoreName(),
                                "tablesDeleted", store.getTables().size(),
                                "qrCodesDeleted", qrCodeMediaIds.size()
                        )
                )
        );

        log.info("Store soft deleted successfully: storeId={}, tables={}, qrCodes={}", 
                storeId, store.getTables().size(), qrCodeMediaIds.size());
    }

    /**
     * Assign menu to store
     */
    @Transactional
    // @CacheEvict(allEntries = true)
    public void assignMenuToStore(UUID storeId, UUID menuId) {
        log.info("Assigning menu to store: storeId={}, menuId={}", storeId, menuId);

        Store store = findStoreById(storeId);
        store.assignMenu(menuId);
        storeRepository.save(store);

        // Publish event
        eventPublisher.publish(new MenuAssignedEvent(
                storeId,
                store.getBusiness().getId(),
                menuId
        ));

        log.info("Menu assigned successfully");
    }

    /**
     * Counts active stores for a business (used by gRPC for subscription limit checks)
     */
    public int countActiveStoresByBusinessId(UUID businessId) {
        log.debug("Counting active stores for business ID: {}", businessId);
        // Count stores that are not DELETED or INACTIVE
        return (int) storeRepository.countByBusinessIdAndStatusNot(businessId, StoreStatus.DELETED);
    }

    // Private helper methods
    private Store findStoreById(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
    }

    private void initializeDefaultStoreSettings(Store store) {
        StoreSettings settings = StoreSettings.builder()
                .store(store)
                .build();
        storeSettingsRepository.save(settings);
        
        UUID storeId = store.getId();
        
        // Initialize all settings with proper defaults
        openingHoursService.initializeDefaultOpeningHours(storeId);
        socialAccountsService.initializeDefaultSocialAccounts(storeId);
        locationDetailsService.initializeDefaultLocationDetails(storeId);
        
        // Initialize default store-level QR customizations (TABLE, WIFI only)
        qrCustomizationService.initializeStoreQRCustomizations(store);
    }
    
}