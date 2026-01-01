package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.CreateWiFiSettingsCommand;
import com.servemenu.businessservice.application.dto.response.WiFiSettingsResponse;
import com.servemenu.businessservice.application.mapper.WiFiSettingsMapper;
import com.servemenu.businessservice.common.exception.DuplicateResourceException;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.event.WifiSettingsDeletedEvent;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.domain.model.BusinessAuditLog;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.model.WifiSettings;
import com.servemenu.businessservice.domain.repository.BusinessAuditLogRepository;
import com.servemenu.businessservice.domain.repository.BusinessRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import com.servemenu.businessservice.domain.repository.WifiSettingsRepository;
import com.servemenu.businessservice.infrastructure.kafka.producer.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WiFiSettingsService {

    private final WifiSettingsRepository wifiSettingsRepository;
    private final StoreRepository storeRepository;
    private final BusinessRepository businessRepository;
    private final BusinessAuditLogRepository auditLogRepository;
    private final QRRequestService qrRequestService;
    private final WiFiSettingsMapper wifiSettingsMapper;
    private final MediaEnrichmentService mediaEnrichmentService;
    private final DomainEventPublisher eventPublisher;

    /**
     * Create WiFi Settings (ASYNC flow with Kafka events)
     * QR generation happens asynchronously via Kafka → QR Service → Media Service
     */
    @Transactional
    public WiFiSettingsResponse createWiFiSettings(
            UUID storeId,
            CreateWiFiSettingsCommand command,
            UUID performedBy
    ) {
        log.info("Creating WiFi settings (async flow): storeId={}, ssid={}", storeId, command.ssid());

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        Business business = businessRepository.findById(store.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Check if WiFi with same name already exists for this store
        if (wifiSettingsRepository.existsByStoreIdAndWifiNameAndIsActiveTrue(storeId, command.wifiName())) {
            throw new DuplicateResourceException("WiFi network with name '" + command.wifiName() + "' already exists for this store");
        }

        // Create WiFi settings entity
        WifiSettings wifiSettings = WifiSettings.builder()
                .store(store)
                .wifiName(command.wifiName())
                .ssid(command.ssid())
                .password(command.password())
                .wifiType(command.wifiType())
                .build();

        // Save WiFi settings first to get the ID
        WifiSettings saved = wifiSettingsRepository.save(wifiSettings);

        // Audit log
        auditLogRepository.save(
                BusinessAuditLog.wifiSettingsCreated(
                        business.getId(),
                        storeId,
                        saved.getId(),
                        performedBy,
                        saved.getSsid()
                )
        );

        // Request WiFi QR code generation via QRRequestService
        try {
            qrRequestService.publishWifiQRRequest(saved, storeId);
            log.info("WiFi QR generation requested: wifiSettingsId={}, storeId={}", saved.getId(), storeId);
        } catch (Exception e) {
            log.error("Failed to request WiFi QR generation: wifiSettingsId={}", saved.getId(), e);
            // Don't fail the WiFi settings creation if QR request fails
        }

        log.info("WiFi settings created (async QR generation): wifiSettingsId={}", saved.getId());

        return wifiSettingsMapper.toResponse(saved);
    }

    /**
     * Get all WiFi networks for a store
     */
    public List<WiFiSettingsResponse> getAllWiFiSettings(UUID storeId) {
        log.debug("Fetching all WiFi networks: storeId={}", storeId);

        List<WifiSettings> wifiList = wifiSettingsRepository.findByStoreIdAndIsActiveTrue(storeId);

        // Enrich each WiFi with QR code URL from Media Service
        wifiList.forEach(wifi -> {
            try {
                mediaEnrichmentService.enrichWifiSettings(wifi);
            } catch (Exception e) {
                log.warn("Failed to enrich WiFi settings with QR URL: wifiId={}", wifi.getId(), e);
            }
        });

        return wifiList.stream()
                .map(wifiSettingsMapper::toResponse)
                .toList();
    }

    /**
     * Get specific WiFi network by ID
     */
    public WiFiSettingsResponse getWiFiSettingsById(UUID wifiId) {
        log.debug("Fetching WiFi network: wifiId={}", wifiId);

        WifiSettings wifiSettings = wifiSettingsRepository.findByIdAndIsActiveTrue(wifiId)
                .orElseThrow(() -> new ResourceNotFoundException("WiFi network not found"));

        // Enrich with QR code URL from Media Service
        try {
            mediaEnrichmentService.enrichWifiSettings(wifiSettings);
        } catch (Exception e) {
            log.warn("Failed to enrich WiFi settings with QR URL: wifiId={}", wifiId, e);
        }

        return wifiSettingsMapper.toResponse(wifiSettings);
    }

    /**
     * Delete WiFi settings
     * Soft deletes WiFi and publishes event for QR code cleanup
     */
    @Transactional
    public void deleteWiFiSettings(UUID wifiSettingsId, UUID performedBy) {
        log.info("Deleting WiFi settings: wifiSettingsId={}", wifiSettingsId);

        WifiSettings wifiSettings = wifiSettingsRepository.findById(wifiSettingsId)
                .orElseThrow(() -> new ResourceNotFoundException("WiFi settings not found"));

        UUID qrCodeMediaId = wifiSettings.getQrCodeMediaId();
        UUID storeId = wifiSettings.getStore().getId();

        // Soft delete - set isActive to false
        wifiSettings.setIsActive(false);
        wifiSettingsRepository.save(wifiSettings);

        // Publish WifiSettingsDeletedEvent if QR code exists (Media Service will clean up)
        if (qrCodeMediaId != null) {
            eventPublisher.publish(new WifiSettingsDeletedEvent(
                    wifiSettingsId,
                    storeId,
                    qrCodeMediaId
            ));
            log.info("Published WifiSettingsDeletedEvent: wifiSettingsId={}, qrCodeMediaId={}", wifiSettingsId, qrCodeMediaId);
        }

        log.info("WiFi settings soft deleted: wifiSettingsId={}", wifiSettingsId);
    }

    /**
     * Update WiFi settings with QR media ID after QR generation
     */
    @Transactional
    public void updateWiFiSettingsWithQRMediaId(UUID wifiSettingsId, UUID qrCodeMediaId) {
        log.info("Updating WiFi settings with QR media ID: wifiSettingsId={}, mediaId={}", wifiSettingsId, qrCodeMediaId);

        WifiSettings wifiSettings = wifiSettingsRepository.findById(wifiSettingsId)
                .orElseThrow(() -> new ResourceNotFoundException("WiFi settings not found"));

        wifiSettings.assignQRCode(qrCodeMediaId);
        wifiSettingsRepository.save(wifiSettings);

        log.info("WiFi settings updated with QR media ID: wifiSettingsId={}", wifiSettingsId);
    }
}
