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
@Table(name = "social_accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_social_accounts_store"))
    private Store store;

    @Column(name = "facebook", length = 500)
    private String facebook;

    @Column(name = "twitter", length = 500)
    private String twitter;

    @Column(name = "instagram", length = 500)
    private String instagram;

    @Column(name = "snapchat", length = 500)
    private String snapchat;

    @Column(name = "pinterest", length = 500)
    private String pinterest;

    @Column(name = "foursquare", length = 500)
    private String foursquare;

    @Column(name = "tripadvisor", length = 500)
    private String tripadvisor;

    @Column(name = "zomato", length = 500)
    private String zomato;

    @Column(name = "tiktok", length = 500)
    private String tiktok;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}