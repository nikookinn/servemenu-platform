package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.LocationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LocationDetailsRepository extends JpaRepository<LocationDetails, UUID> {

    Optional<LocationDetails> findByStoreId(UUID storeId);

    boolean existsByStoreId(UUID storeId);

    @Query("SELECT ld FROM LocationDetails ld WHERE ld.isEnabled = true AND ld.store.id = :storeId")
    Optional<LocationDetails> findEnabledByStoreId(@Param("storeId") UUID storeId);
}
