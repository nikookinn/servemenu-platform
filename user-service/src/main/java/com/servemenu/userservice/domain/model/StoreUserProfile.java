package com.servemenu.userservice.domain.model;

import com.servemenu.userservice.domain.enums.StoreAccessLevel;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "store_user_profiles",
        indexes = {
                @Index(name = "idx_store_user_profiles_user_id", columnList = "user_id"),
                @Index(name = "idx_store_user_profiles_business_id", columnList = "business_id"),
                @Index(name = "idx_store_user_profiles_store_id", columnList = "store_id"),
                @Index(name = "idx_store_user_profiles_business_store", columnList = "business_id, store_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class StoreUserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_store_user_profile_user"))
    private User user;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 50)
    private StoreAccessLevel accessLevel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StoreUserProfile() {
    }

    public StoreUserProfile(UUID id,
                            User user,
                            UUID businessId,
                            UUID storeId,
                            StoreAccessLevel accessLevel,
                            Boolean isActive,
                            UUID createdByUserId,
                            Instant createdAt) {
        this.id = id;
        this.user = user;
        this.businessId = businessId;
        this.storeId = storeId;
        this.accessLevel = accessLevel;
        this.isActive = isActive;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    // Business methods
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void changeAccessLevel(StoreAccessLevel newAccessLevel) {
        if (this.accessLevel == newAccessLevel) {
            throw new IllegalArgumentException("New access level is same as current");
        }
        this.accessLevel = newAccessLevel;
    }

    public boolean isAdmin() {
        return StoreAccessLevel.STORE_ADMIN.equals(this.accessLevel);
    }

    public boolean hasAccessToStore(UUID storeId) {
        return this.storeId.equals(storeId) && Boolean.TRUE.equals(this.isActive);
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

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public StoreAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(StoreAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreUserProfile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}