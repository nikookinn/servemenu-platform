package com.servemenu.businessservice.domain.model;


import com.servemenu.businessservice.domain.enums.StoreStatus;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "stores",
        indexes = {
                @Index(name = "idx_store_business_id", columnList = "business_id"),
                @Index(name = "idx_store_business_status", columnList = "business_id, status"),
                @Index(name = "idx_store_is_default", columnList = "is_default"),
                @Index(name = "idx_store_business_code", columnList = "business_id, store_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_business_code", columnNames = {"business_id", "store_code"})
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_business"))
    private Business business;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "store_code", length = 4, nullable = false)
    private String storeCode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "menu_id")
    private UUID menuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private StoreStatus status = StoreStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.servemenu.businessservice.domain.model.Table> tables = new ArrayList<>();

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private StoreSettings settings;

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private SocialAccounts socialAccounts;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WifiSettings> wifiSettings = new ArrayList<>();

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private LocationDetails locationDetails;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OpeningHours> openingHours = new ArrayList<>();
    
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QRCustomization> qrCustomizations = new ArrayList<>();

    // Business Logic Methods
    public void softDelete() {
        this.status = StoreStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public void activate() {
        this.status = StoreStatus.ACTIVE;
        this.deletedAt = null;
    }

    public void deactivate() {
        this.status = StoreStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == StoreStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == StoreStatus.DELETED;
    }

    public void assignMenu(UUID menuId) {
        this.menuId = menuId;
    }

    public void updateContactInfo(String phoneNumber, String countryCode) {
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
    }
}