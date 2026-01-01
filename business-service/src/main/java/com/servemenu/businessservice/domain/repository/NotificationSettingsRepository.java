package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {

    Optional<NotificationSettings> findByBusinessId(UUID businessId);

    boolean existsByBusinessId(UUID businessId);
}
