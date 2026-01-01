package com.servemenu.businessservice.domain.model;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_store_settings_store"))
    private Store store;

    // QR Menu Delivery Methods
    @Column(name = "enable_dine_in", nullable = false)
    @Builder.Default
    private Boolean enableDineIn = true;

    @Column(name = "enable_takeaway", nullable = false)
    @Builder.Default
    private Boolean enableTakeaway = false;

    // Online Menu Delivery Methods
    @Column(name = "enable_pickup", nullable = false)
    @Builder.Default
    private Boolean enablePickup = false;

    @Column(name = "enable_delivery", nullable = false)
    @Builder.Default
    private Boolean enableDelivery = false;

    @Column(name = "enable_guest_checkout", nullable = false)
    @Builder.Default
    private Boolean enableGuestCheckout = true;

    @Column(name = "allow_special_instructions", nullable = false)
    @Builder.Default
    private Boolean allowSpecialInstructions = true;

    @Column(name = "display_full_food_name", nullable = false)
    @Builder.Default
    private Boolean displayFullFoodName = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}