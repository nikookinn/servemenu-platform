package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.model.StoreUserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreUserProfileRepository extends JpaRepository<StoreUserProfile, UUID> {
    @Query("SELECT s FROM StoreUserProfile s WHERE s.user.id = :userId")
    Optional<StoreUserProfile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM StoreUserProfile s WHERE s.storeId = :storeId AND s.isActive = true")
    Page<StoreUserProfile> findActiveByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
    
    @Query("SELECT s FROM StoreUserProfile s WHERE s.storeId = :storeId AND s.isActive = :isActive")
    List<StoreUserProfile> findByStoreIdAndIsActive(@Param("storeId") UUID storeId, @Param("isActive") Boolean isActive);

    @Query("SELECT s FROM StoreUserProfile s WHERE s.businessId = :businessId")
    List<StoreUserProfile> findByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT s FROM StoreUserProfile s WHERE s.businessId = :businessId AND s.storeId = :storeId")
    List<StoreUserProfile> findByBusinessIdAndStoreId(@Param("businessId") UUID businessId, @Param("storeId") UUID storeId);

    @Query("SELECT COUNT(s) FROM StoreUserProfile s WHERE s.storeId = :storeId AND s.isActive = true")
    long countActiveByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(s) > 0 FROM StoreUserProfile s WHERE s.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}
