package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, UUID> {

    Optional<StoreSettings> findByStoreId(UUID storeId);

    boolean existsByStoreId(UUID storeId);
}
