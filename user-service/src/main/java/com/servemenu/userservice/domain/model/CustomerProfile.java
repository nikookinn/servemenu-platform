package com.servemenu.userservice.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "customer_profiles",
        indexes = {
                @Index(name = "idx_customer_profiles_user_id", columnList = "user_id"),
                @Index(name = "idx_customer_profiles_business_id", columnList = "created_by_business_id"),
                @Index(name = "idx_customer_profiles_phone", columnList = "phone_number")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_customer_profile_user"))
    private User user;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "preferred_language", length = 5, nullable = false)
    private String preferredLanguage = "en";

    @Column(name = "created_by_business_id")
    private UUID createdByBusinessId;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerProfile() {
    }

    public CustomerProfile(UUID id,
                           User user,
                           String phoneNumber,
                           String countryCode,
                           String preferredLanguage,
                           UUID createdByBusinessId,
                           Integer loyaltyPoints,
                           Instant createdAt,
                           Instant updatedAt) {
        this.id = id;
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        this.preferredLanguage = preferredLanguage;
        this.createdByBusinessId = createdByBusinessId;
        this.loyaltyPoints = loyaltyPoints;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Business methods
    public void updatePhoneNumber(String phoneNumber, String countryCode) {
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
    }

    public void changePreferredLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }
        this.preferredLanguage = language;
    }

    public void addLoyaltyPoints(Integer points) {
        if (points == null || points < 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        this.loyaltyPoints += points;
    }

    public void redeemLoyaltyPoints(Integer points) {
        if (points == null || points < 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        if (this.loyaltyPoints < points) {
            throw new IllegalStateException(
                    String.format("Insufficient loyalty points. Available: %d, Requested: %d",
                            this.loyaltyPoints, points)
            );
        }
        this.loyaltyPoints -= points;
    }

    public void resetLoyaltyPoints() {
        this.loyaltyPoints = 0;
    }

    public String getFullPhoneNumber() {
        if (phoneNumber == null) {
            return null;
        }
        return countryCode != null ? countryCode + phoneNumber : phoneNumber;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public UUID getCreatedByBusinessId() {
        return createdByBusinessId;
    }

    public void setCreatedByBusinessId(UUID createdByBusinessId) {
        this.createdByBusinessId = createdByBusinessId;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerProfile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}