package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.enums.QRType;
import com.servemenu.businessservice.domain.model.QRCustomization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QRCustomizationRepository extends JpaRepository<QRCustomization, UUID> {

    // ========== Business-Level QR (BUSINESS type) ==========
    
    /**
     * Find BUSINESS QR customization by business ID
     */
    Optional<QRCustomization> findByBusinessIdAndQrType(UUID businessId, QRType qrType);
    
    /**
     * Check if BUSINESS QR customization exists
     */
    boolean existsByBusinessIdAndQrType(UUID businessId, QRType qrType);
    
    /**
     * Delete BUSINESS QR customization
     */
    void deleteByBusinessIdAndQrType(UUID businessId, QRType qrType);

    // ========== Store-Level QR (TABLE, WIFI types) ==========
    
    /**
     * Find QR customization by store ID and QR type
     * Used for TABLE and WIFI QR types
     */
    Optional<QRCustomization> findByStoreIdAndQrType(UUID storeId, QRType qrType);

    /**
     * Find all QR customizations for a store (TABLE, WIFI)
     */
    List<QRCustomization> findByStoreId(UUID storeId);

    /**
     * Check if QR customization exists for store and type
     */
    boolean existsByStoreIdAndQrType(UUID storeId, QRType qrType);

    /**
     * Delete all QR customizations for a store
     */
    void deleteByStoreId(UUID storeId);
    
    // ========== Unified Query ==========
    
    /**
     * Find QR customization by business or store ID and type
     * Handles both business-level (BUSINESS) and store-level (TABLE, WIFI) QR
     */
    @Query("SELECT qc FROM QRCustomization qc WHERE " +
           "(qc.business.id = :businessId AND qc.qrType = :qrType AND qc.qrType = 'BUSINESS') OR " +
           "(qc.store.id = :storeId AND qc.qrType = :qrType AND qc.qrType IN ('TABLE', 'WIFI'))")
    Optional<QRCustomization> findByBusinessOrStoreAndQrType(
            @Param("businessId") UUID businessId,
            @Param("storeId") UUID storeId,
            @Param("qrType") QRType qrType
    );
}
