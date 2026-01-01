package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.BusinessAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BusinessAuditLogRepository extends JpaRepository<BusinessAuditLog, Long> {

    Page<BusinessAuditLog> findByBusinessIdOrderByTimestampDesc(UUID businessId, Pageable pageable);

    Page<BusinessAuditLog> findByStoreIdOrderByTimestampDesc(UUID storeId, Pageable pageable);

    Page<BusinessAuditLog> findByPerformedByOrderByTimestampDesc(UUID performedBy, Pageable pageable);

    @Query("SELECT bal FROM BusinessAuditLog bal WHERE bal.businessId = :businessId AND bal.action = :action ORDER BY bal.timestamp DESC")
    List<BusinessAuditLog> findByBusinessIdAndAction(
            @Param("businessId") UUID businessId,
            @Param("action") String action
    );

    @Query("SELECT COUNT(bal) FROM BusinessAuditLog bal WHERE bal.businessId = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);
}
