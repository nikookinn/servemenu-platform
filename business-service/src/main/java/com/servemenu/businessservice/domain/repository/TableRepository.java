package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableRepository extends JpaRepository<Table, UUID> {

    // Soft delete aware queries
    Page<Table> findByStoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    List<Table> findByStoreIdAndIsActiveAndDeletedAtIsNull(UUID storeId, Boolean isActive);

    Optional<Table> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Table> findByQrCodeMediaId(UUID qrCodeMediaId);

    long countByStoreIdAndDeletedAtIsNull(UUID storeId);

    // Legacy methods (kept for backward compatibility)
    @Deprecated
    Page<Table> findByStoreId(UUID storeId, Pageable pageable);

    @Deprecated
    List<Table> findByStoreIdAndIsActive(UUID storeId, Boolean isActive);

    @Deprecated
    long countByStoreId(UUID storeId);

    boolean existsByStoreIdAndTableName(UUID storeId, String tableName);

    Optional<Table> findByStoreIdAndTableNumber(UUID storeId, String tableNumber);

    @Query("SELECT t FROM Table t WHERE t.store.id = :storeId ORDER BY t.createdAt DESC")
    List<Table> findByStoreIdOrderByCreatedAtDesc(@Param("storeId") UUID storeId);

    @Query("SELECT MAX(CAST(SUBSTRING(t.tableNumber, 2) AS int)) FROM Table t WHERE t.store.id = :storeId")
    Integer findMaxTableNumberByStoreId(@Param("storeId") UUID storeId);

    /**
     * Optimized QR URL lookup - Single JOIN query
     * Fetches table with store and business in ONE query
     * Used for QR code scanning - performance critical
     */
    @Query("""
        SELECT t FROM Table t 
        JOIN FETCH t.store s 
        JOIN FETCH s.business b 
        WHERE b.slug = :businessSlug 
        AND s.storeCode = :storeCode 
        AND t.tableNumber = :tableNumber
    """)
    Optional<Table> findByBusinessSlugAndStoreCodeAndTableNumber(
        @Param("businessSlug") String businessSlug,
        @Param("storeCode") String storeCode,
        @Param("tableNumber") String tableNumber
    );
}

