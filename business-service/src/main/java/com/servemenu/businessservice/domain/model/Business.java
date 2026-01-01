package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.domain.enums.*;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "businesses",
        indexes = {
                @Index(name = "idx_business_owner_id", columnList = "business_owner_id"),
                @Index(name = "idx_business_keycloak_user_id", columnList = "keycloak_user_id"),
                @Index(name = "idx_business_slug", columnList = "slug"),
                @Index(name = "idx_business_custom_domain", columnList = "custom_domain"),
                @Index(name = "idx_business_owner_status", columnList = "business_owner_id, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_owner_id", nullable = false)
    private UUID businessOwnerId;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private UUID keycloakUserId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 50)
    private BusinessType businessType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_languages", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> supportedLanguages = new ArrayList<>();

    @Column(name = "default_language", nullable = false, length = 5)
    private String defaultLanguage;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "custom_domain", unique = true)
    private String customDomain;

    @Column(name = "custom_domain_verified")
    @Builder.Default
    private Boolean customDomainVerified = false;

    @Column(name = "is_onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean isOnboardingCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private BusinessStatus status = BusinessStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Business QR Code Media ID (from Media Service)
    @Column(name = "business_qr_media_id")
    private UUID businessQrMediaId;

    // Relationships
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Store> stores = new ArrayList<>();

    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private BusinessSettings businessSettings;

    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotificationSettings notificationSettings;

    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderSettings orderSettings;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tax> taxes = new ArrayList<>();

    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomDomain customDomainDetails;

    // Business Logic Methods
    public void completeOnboarding() {
        this.isOnboardingCompleted = true;
    }

    public boolean canCreateMoreStores() {
        int maxStores = subscriptionPlan.getMaxStores();
        if (maxStores == -1) return true; // Unlimited
        return stores.stream()
                .filter(s -> s.getStatus() != StoreStatus.DELETED)
                .count() < maxStores;
    }

    public void softDelete() {
        this.status = BusinessStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public void suspend() {
        this.status = BusinessStatus.SUSPENDED;
    }

    public void activate() {
        this.status = BusinessStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.status == BusinessStatus.DELETED;
    }

    public boolean isActive() {
        return this.status == BusinessStatus.ACTIVE;
    }

    public void addLanguage(String languageCode) {
        if (!supportedLanguages.contains(languageCode)) {
            supportedLanguages.add(languageCode);
        }
    }

    public void removeLanguage(String languageCode) {
        if (!languageCode.equals(defaultLanguage)) {
            supportedLanguages.remove(languageCode);
        }
    }

    public void updateSubscriptionPlan(SubscriptionPlan newPlan) {
        this.subscriptionPlan = newPlan;
    }
}