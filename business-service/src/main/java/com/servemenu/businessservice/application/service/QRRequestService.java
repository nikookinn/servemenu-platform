package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.domain.enums.QRType;
import com.servemenu.businessservice.domain.event.QRGenerationRequestedEvent;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.Table;
import com.servemenu.businessservice.domain.model.WifiSettings;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * QR Request Service
 * 
 * Centralized service for managing QR generation requests across all QR types.
 * Publishes QRGenerationRequestedEvent to Kafka for QR Service to consume.
 * Does NOT generate QR codes - only coordinates requests with proper URLs and metadata.
 * 
 * Supports 3 QR types:
 * - BUSINESS: Business landing page QR (business-level)
 * - TABLE: Table menu QR (store-level)
 * - WIFI: WiFi credentials QR (store-level)
 * 
 * Architecture Benefits:
 * - Single Responsibility: All QR request logic in one place
 * - Consistency: Same pattern for all QR types
 * - Maintainability: Easy to modify QR generation flow
 * - Testability: Isolated QR request logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QRRequestService {

    private final DomainEventPublisher domainEventPublisher;
    private final QRUrlService qrUrlService;

    /**
     * Publish BUSINESS QR generation request (business-level)
     * Called when business is created or business QR needs regeneration
     * Note: BUSINESS QR is not tied to any specific store
     */
    public void publishBusinessQRRequest(Business business) {
        log.info("Publishing BUSINESS QR generation request: businessId={}", business.getId());

        // Generate business URL
        String businessUrl = qrUrlService.generateBusinessQRUrl(business);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("businessName", business.getBusinessName());
        metadata.put("businessSlug", business.getSlug());
        metadata.put("displayName", business.getBusinessName()); // For filename in ZIP

        // Build and publish event
        // Note: storeId is null for BUSINESS QR since it's business-level
        QRGenerationRequestedEvent event = new QRGenerationRequestedEvent(
                null,
                business.getId(),
                QRType.BUSINESS,
                businessUrl,
                metadata
        );

        domainEventPublisher.publish(event);
        
        log.info("✅ Published BUSINESS QR generation request: businessId={}, url={}", 
                business.getId(), businessUrl);
    }

    /**
     * Publish WIFI QR generation request (store-level)
     * Called when WiFi settings are updated AND QR customization exists
     * Note: QR customization must exist before calling this method
     */
    public void publishWifiQRRequest(WifiSettings wifiSettings, UUID storeId) {
        log.info("Publishing WIFI QR generation request: wifiSettingsId={}, storeId={}", 
                wifiSettings.getId(), storeId);

        // Get businessId from wifiSettings.store.business
        UUID businessId = wifiSettings.getStore().getBusiness().getId();

        // Generate WiFi QR string
        String wifiQRString = qrUrlService.generateWifiQRString(wifiSettings);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("wifiSettingsId", wifiSettings.getId().toString());
        metadata.put("ssid", wifiSettings.getSsid());
        metadata.put("wifiType", wifiSettings.getWifiType().name());
        metadata.put("displayName", wifiSettings.getSsid()); // For filename in ZIP

        // Build and publish event
        QRGenerationRequestedEvent event = new QRGenerationRequestedEvent(
                storeId,
                businessId, // ✅ businessId is required for QR Service to fetch customization
                QRType.WIFI,
                wifiQRString,
                metadata
        );

        domainEventPublisher.publish(event);
        
        log.info("✅ Published WIFI QR generation request: wifiSettingsId={}, ssid={}, businessId={}", 
                wifiSettings.getId(), wifiSettings.getSsid(), businessId);
    }

    /**
     * Publish TABLE QR generation request (store-level)
     * Called when a table is created or needs QR regeneration
     */
    public void publishTableQRRequest(Table table, String qrUrl) {
        log.info("Publishing TABLE QR generation request: tableId={}, storeId={}", 
                table.getId(), table.getStore().getId());

        // Get businessId from table.store.business
        UUID businessId = table.getStore().getBusiness().getId();
        UUID storeId = table.getStore().getId();

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableId", table.getId().toString());
        metadata.put("tableName", table.getTableName());
        metadata.put("tableNumber", table.getTableNumber());
        metadata.put("displayName", table.getTableName()); // For filename in ZIP

        // Build and publish event
        QRGenerationRequestedEvent event = new QRGenerationRequestedEvent(
                storeId,
                businessId,
                QRType.TABLE,
                qrUrl,
                metadata
        );

        domainEventPublisher.publish(event);
        
        log.info("✅ Published TABLE QR generation request: tableId={}, tableName={}, url={}", 
                table.getId(), table.getTableName(), qrUrl);
    }
}
