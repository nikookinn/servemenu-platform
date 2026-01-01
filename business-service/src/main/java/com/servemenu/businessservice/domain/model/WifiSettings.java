package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.domain.enums.WifiType;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wifi_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wifi_settings_store"))
    private Store store;

    @Column(name = "wifi_name", nullable = false, length = 100)
    private String wifiName; // "Guest WiFi", "Staff WiFi", etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "wifi_type", nullable = false, length = 50)
    private WifiType wifiType;

    @Column(name = "ssid", nullable = false)
    private String ssid;

    @Column(name = "password")
    private String password;

    // Media ID (stored in database - reference to Media Service)
    // WiFi QR customization is found via store_id + qr_type='WIFI' in qr_customizations table
    @Column(name = "qr_code_media_id")
    private UUID qrCodeMediaId;

    // Soft delete flag
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Transient field (not persisted - enriched at runtime via gRPC)
    @Transient
    private String qrCodeUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Business Logic Methods
    public void assignQRCode(UUID qrCodeMediaId) {
        this.qrCodeMediaId = qrCodeMediaId;
    }

    public void updateWifiCredentials(String ssid, String password, WifiType wifiType) {
        this.ssid = ssid;
        this.password = password;
        this.wifiType = wifiType;
    }
}