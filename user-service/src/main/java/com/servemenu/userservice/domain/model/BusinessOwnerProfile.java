package com.servemenu.userservice.domain.model;

import com.servemenu.userservice.domain.enums.SubscriptionPlan;
import com.servemenu.userservice.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "business_owner_profiles",
        indexes = {
                @Index(name = "idx_business_owner_profiles_user_id", columnList = "user_id"),
                @Index(name = "idx_business_owner_profiles_onboarding", columnList = "onboarding_completed")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class BusinessOwnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_business_owner_profile_user"))
    private User user;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @Column(name = "business_id")
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 50)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "subscription_starts_at")
    private Instant subscriptionStartsAt;

    @Column(name = "subscription_ends_at")
    private Instant subscriptionEndsAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BusinessOwnerProfile() {
    }

    public BusinessOwnerProfile(UUID id,
                                User user,
                                Boolean onboardingCompleted,
                                SubscriptionPlan subscriptionPlan,
                                SubscriptionStatus subscriptionStatus,
                                Instant trialEndsAt,
                                Instant subscriptionStartsAt,
                                Instant subscriptionEndsAt,
                                Instant createdAt,
                                Instant updatedAt) {
        this.id = id;
        this.user = user;
        this.onboardingCompleted = onboardingCompleted;
        this.subscriptionPlan = subscriptionPlan;
        this.subscriptionStatus = subscriptionStatus;
        this.trialEndsAt = trialEndsAt;
        this.subscriptionStartsAt = subscriptionStartsAt;
        this.subscriptionEndsAt = subscriptionEndsAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Business methods
    /**
     * Complete onboarding and set business ID
     * Called when BusinessDetailsCompletedEvent is received from business-service
     */
    public void completeOnboarding() {
        if (Boolean.TRUE.equals(this.onboardingCompleted)) {
            throw new IllegalStateException("Onboarding has already been completed");
        }
        this.onboardingCompleted = true;
    }

    public void upgradePlan(SubscriptionPlan newPlan) {
        if (newPlan.ordinal() <= this.subscriptionPlan.ordinal()) {
            throw new IllegalArgumentException("New plan must be higher than current plan");
        }
        this.subscriptionPlan = newPlan;
        this.subscriptionStartsAt = Instant.now();
        this.subscriptionStatus = SubscriptionStatus.ACTIVE;
    }

    public void downgradePlan(SubscriptionPlan newPlan) {
        if (newPlan.ordinal() >= this.subscriptionPlan.ordinal()) {
            throw new IllegalArgumentException("New plan must be lower than current plan");
        }
        this.subscriptionPlan = newPlan;
        this.subscriptionStartsAt = Instant.now();
    }

    public void cancelSubscription() {
        this.subscriptionStatus = SubscriptionStatus.CANCELLED;
        this.subscriptionEndsAt = Instant.now();
    }

    public void activateSubscription() {
        this.subscriptionStatus = SubscriptionStatus.ACTIVE;
    }

    public void expireSubscription() {
        this.subscriptionStatus = SubscriptionStatus.EXPIRED;
        this.subscriptionEndsAt = Instant.now();
    }

    public boolean hasActiveSubscription() {
        return SubscriptionStatus.ACTIVE.equals(this.subscriptionStatus);
    }

    public boolean isOnTrial() {
        return SubscriptionStatus.TRIAL.equals(this.subscriptionStatus);
    }

    public boolean isFreePlan() {
        return SubscriptionPlan.FREE.equals(this.subscriptionPlan);
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

    public Boolean getOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(Boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public Instant getSubscriptionStartsAt() {
        return subscriptionStartsAt;
    }

    public void setSubscriptionStartsAt(Instant subscriptionStartsAt) {
        this.subscriptionStartsAt = subscriptionStartsAt;
    }

    public Instant getSubscriptionEndsAt() {
        return subscriptionEndsAt;
    }

    public void setSubscriptionEndsAt(Instant subscriptionEndsAt) {
        this.subscriptionEndsAt = subscriptionEndsAt;
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
        if (!(o instanceof BusinessOwnerProfile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}