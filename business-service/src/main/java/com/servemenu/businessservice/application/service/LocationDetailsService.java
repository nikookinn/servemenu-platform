package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.UpdateLocationDetailsCommand;
import com.servemenu.businessservice.application.dto.response.LocationDetailsResponse;
import com.servemenu.businessservice.application.mapper.SettingsMapper;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.model.LocationDetails;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.repository.LocationDetailsRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Location Details Service
 * Manages store location settings (GPS coordinates, radius, etc.)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class LocationDetailsService {

    private final LocationDetailsRepository locationDetailsRepository;
    private final StoreRepository storeRepository;
    private final SettingsMapper settingsMapper;

    /**
     * Update location details
     */
    @Transactional
    @CacheEvict(value = "store", allEntries = true)
    public LocationDetailsResponse updateLocationDetails(
            UUID storeId,
            UpdateLocationDetailsCommand command
    ) {
        log.info("Updating location details: storeId={}", storeId);

        LocationDetails location = locationDetailsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultLocationDetails(storeId));

        if (command.isEnabled() != null) {
            if (command.isEnabled()) {
                location.enable();
            } else {
                location.disable();
            }
        }

        if (command.latitude() != null) location.setLatitude(command.latitude());
        if (command.longitude() != null) location.setLongitude(command.longitude());
        if (command.radiusInMeters() != null) location.setRadiusInMeters(command.radiusInMeters());

        LocationDetails saved = locationDetailsRepository.save(location);
        return settingsMapper.toLocationDetailsResponse(saved);
    }

    /**
     * Get location details
     */
    public LocationDetailsResponse getLocationDetails(UUID storeId) {
        log.debug("Fetching location details: storeId={}", storeId);
        
        LocationDetails location = locationDetailsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultLocationDetails(storeId));
        return settingsMapper.toLocationDetailsResponse(location);
    }

    /**
     * Create default location details
     */
    private LocationDetails createDefaultLocationDetails(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        LocationDetails locationDetails = LocationDetails.builder()
                .store(store)
                .build();
        return locationDetailsRepository.save(locationDetails);
    }

    /**
     * Initialize default location details for a new store
     */
    @Transactional
    public void initializeDefaultLocationDetails(UUID storeId) {
        log.info("Initializing default location details: storeId={}", storeId);
        createDefaultLocationDetails(storeId);
        log.info("✅ Default location details initialized: storeId={}", storeId);
    }
}
