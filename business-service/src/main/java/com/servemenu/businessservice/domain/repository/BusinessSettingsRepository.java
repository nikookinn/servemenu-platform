package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, UUID> {

    Optional<BusinessSettings> findByBusinessId(UUID businessId);

    boolean existsByBusinessId(UUID businessId);
}
