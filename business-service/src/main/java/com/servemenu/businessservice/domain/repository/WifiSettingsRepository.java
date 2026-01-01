package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.WifiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WifiSettingsRepository extends JpaRepository<WifiSettings, UUID> {

    // Find all active WiFi networks for a store
    List<WifiSettings> findByStoreIdAndIsActiveTrue(UUID storeId);
    
    // Find specific WiFi by ID and active status
    Optional<WifiSettings> findByIdAndIsActiveTrue(UUID id);
    
    // Find specific WiFi by store and name
    Optional<WifiSettings> findByStoreIdAndWifiNameAndIsActiveTrue(UUID storeId, String wifiName);
    
    // Check if WiFi name exists in store
    boolean existsByStoreIdAndWifiNameAndIsActiveTrue(UUID storeId, String wifiName);
}
