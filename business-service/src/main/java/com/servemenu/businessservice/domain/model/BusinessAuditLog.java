package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.common.util.RequestContextUtil;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "business_audit_log",
        indexes = {
                @Index(name = "idx_audit_log_business_id", columnList = "business_id"),
                @Index(name = "idx_audit_log_store_id", columnList = "store_id"),
                @Index(name = "idx_audit_log_performed_by", columnList = "performed_by"),
                @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_log_business_timestamp", columnList = "business_id, timestamp")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy; // userId

    @Column(name = "action", nullable = false, length = 100)
    private String action; // BUSINESS_CREATED, STORE_UPDATED, TABLE_DELETED, etc.

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // Business, Store, Table, Settings, etc.

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    // Static factory methods for common audit events
    public static BusinessAuditLog businessCreated(
            UUID businessId,
            UUID performedBy,
            String businessName
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .performedBy(performedBy)
                .action("BUSINESS_CREATED")
                .entityType("Business")
                .entityId(businessId)
                .changes(Map.of("businessName", businessName))
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog storeCreated(
            UUID businessId,
            UUID storeId,
            UUID performedBy,
            String storeName
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("STORE_CREATED")
                .entityType("Store")
                .entityId(storeId)
                .changes(Map.of("storeName", storeName))
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog storeUpdated(
            UUID businessId,
            UUID storeId,
            UUID performedBy,
            Map<String, Object> changes
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("STORE_UPDATED")
                .entityType("Store")
                .entityId(storeId)
                .changes(changes)
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog storeDeleted(
            UUID businessId,
            UUID storeId,
            UUID performedBy,
            Map<String, Object> changes
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("STORE_DELETED")
                .entityType("Store")
                .entityId(storeId)
                .changes(changes)
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog tableCreated(
            UUID businessId,
            UUID storeId,
            UUID tableId,
            UUID performedBy,
            String tableName
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("TABLE_CREATED")
                .entityType("Table")
                .entityId(tableId)
                .changes(Map.of("tableName", tableName))
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog wifiSettingsCreated(
            UUID businessId,
            UUID storeId,
            UUID wifiSettingsId,
            UUID performedBy,
            String ssid
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("WIFI_SETTINGS_CREATED")
                .entityType("WifiSettings")
                .entityId(wifiSettingsId)
                .changes(Map.of("ssid", ssid))
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }

    public static BusinessAuditLog settingsUpdated(
            UUID businessId,
            UUID storeId,
            UUID performedBy,
            String settingsType,
            Map<String, Object> changes
    ) {
        return BusinessAuditLog.builder()
                .businessId(businessId)
                .storeId(storeId)
                .performedBy(performedBy)
                .action("SETTINGS_UPDATED")
                .entityType(settingsType)
                .changes(changes)
                .ipAddress(RequestContextUtil.getClientIpAddress())
                .userAgent(RequestContextUtil.getUserAgent())
                .build();
    }
}