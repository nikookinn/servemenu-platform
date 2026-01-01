package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.enums.StoreStatus;
import com.servemenu.businessservice.domain.model.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    Page<Store> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Store> findByBusinessIdAndStatusNot(UUID businessId, StoreStatus status, Pageable pageable);

    List<Store> findByBusinessIdAndStatus(UUID businessId, StoreStatus status);

    Optional<Store> findByBusinessIdAndIsDefaultTrue(UUID businessId);

    long countByBusinessIdAndStatusNot(UUID businessId, StoreStatus status);

    boolean existsByBusinessIdAndStoreName(UUID businessId, String storeName);

    boolean existsByBusinessIdAndStoreCode(UUID businessId, String storeCode);

    Optional<Store> findByBusinessIdAndStoreCode(UUID businessId, String storeCode);

    @Query("SELECT s FROM Store s " +
            "LEFT JOIN FETCH s.settings " +
            "LEFT JOIN FETCH s.socialAccounts " +
            "LEFT JOIN FETCH s.wifiSettings " +
            "LEFT JOIN FETCH s.locationDetails " +
            "WHERE s.id = :storeId")
    Optional<Store> findByIdWithSettings(@Param("storeId") UUID storeId);

    @Query("SELECT s FROM Store s " +
            "LEFT JOIN FETCH s.tables " +
            "WHERE s.id = :storeId")
    Optional<Store> findByIdWithTables(@Param("storeId") UUID storeId);

    @Query("SELECT s FROM Store s WHERE s.business.id = :businessId AND s.status = :status")
    List<Store> findActiveStoresByBusinessId(
            @Param("businessId") UUID businessId,
            @Param("status") StoreStatus status
    );
}
