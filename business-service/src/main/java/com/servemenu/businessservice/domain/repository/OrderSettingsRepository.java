package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.OrderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderSettingsRepository extends JpaRepository<OrderSettings, UUID> {

    Optional<OrderSettings> findByBusinessId(UUID businessId);

    boolean existsByBusinessId(UUID businessId);
}
