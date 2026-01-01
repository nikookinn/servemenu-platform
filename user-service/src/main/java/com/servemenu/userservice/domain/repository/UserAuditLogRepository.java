package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.model.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, UUID> {
    @Query("SELECT a FROM UserAuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    Page<UserAuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM UserAuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<UserAuditLog> findByUserIdAndTimestampBetween(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT a FROM UserAuditLog a WHERE a.action = :action ORDER BY a.timestamp DESC")
    Page<UserAuditLog> findByAction(@Param("action") String action, Pageable pageable);
}
