package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.Tax;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxRepository extends JpaRepository<Tax, UUID> {

    List<Tax> findByBusinessIdAndIsActive(UUID businessId, Boolean isActive);

    List<Tax> findByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    Optional<Tax> findByBusinessIdAndId(UUID businessId, UUID taxId);

    boolean existsByBusinessIdAndName(UUID businessId, String name);

    long countByBusinessIdAndIsActive(UUID businessId, Boolean isActive);
}
