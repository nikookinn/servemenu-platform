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
@Table(name = "business_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_restaurant_settings_business"))
    private Business business;

    // Media IDs (stored in database - references to Media Service)
    @Column(name = "logo_media_id")
    private UUID logoMediaId;

    @Column(name = "cover_image_media_id")
    private UUID coverImageMediaId;

    // Transient fields (not persisted - enriched at runtime via gRPC)
    @Transient
    private String logoUrl;

    @Transient
    private String coverImageUrl;

    @Column(name = "address")
    private String address;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "enable_default_food_image", nullable = false)
    @Builder.Default
    private Boolean enableDefaultFoodImage = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}