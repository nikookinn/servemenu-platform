package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.UpdateStoreSettingsCommand;
import com.servemenu.businessservice.application.dto.response.StoreSettingsResponse;
import com.servemenu.businessservice.application.mapper.SettingsMapper;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.model.StoreSettings;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import com.servemenu.businessservice.domain.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Store Settings Service
 * Manages store-level settings (delivery methods, food display, etc.)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;
    private final StoreRepository storeRepository;
    private final SettingsMapper settingsMapper;

    /**
     * Update store settings
     */
    @Transactional
    @CacheEvict(value = "store", allEntries = true)
    public StoreSettingsResponse updateStoreSettings(
            UUID storeId,
            UpdateStoreSettingsCommand command
    ) {
        log.info("Updating store settings: storeId={}", storeId);

        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultStoreSettings(storeId));

        if (command.enableDineIn() != null) settings.setEnableDineIn(command.enableDineIn());
        if (command.enableTakeaway() != null) settings.setEnableTakeaway(command.enableTakeaway());
        if (command.enablePickup() != null) settings.setEnablePickup(command.enablePickup());
        if (command.enableDelivery() != null) settings.setEnableDelivery(command.enableDelivery());
        if (command.enableGuestCheckout() != null) settings.setEnableGuestCheckout(command.enableGuestCheckout());
        if (command.allowSpecialInstructions() != null) settings.setAllowSpecialInstructions(command.allowSpecialInstructions());
        if (command.displayFullFoodName() != null) settings.setDisplayFullFoodName(command.displayFullFoodName());

        StoreSettings saved = storeSettingsRepository.save(settings);
        return settingsMapper.toStoreSettingsResponse(saved);
    }

    /**
     * Get store settings
     */
    public StoreSettingsResponse getStoreSettings(UUID storeId) {
        log.debug("Fetching store settings: storeId={}", storeId);
        
        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store settings not found"));
        return settingsMapper.toStoreSettingsResponse(settings);
    }

    /**
     * Create default store settings
     */
    private StoreSettings createDefaultStoreSettings(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        return StoreSettings.builder().store(store).build();
    }
}
