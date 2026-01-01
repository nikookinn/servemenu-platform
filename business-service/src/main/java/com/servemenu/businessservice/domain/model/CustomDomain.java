package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.domain.enums.DomainVerificationStatus;
import com.servemenu.businessservice.domain.enums.SslStatus;
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
        name = "custom_domains",
        indexes = {
                @Index(name = "idx_custom_domain_business_id", columnList = "business_id"),
                @Index(name = "idx_custom_domain_domain", columnList = "domain")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_custom_domain_business"))
    private Business business;

    @Column(name = "domain", nullable = false, unique = true)
    private String domain;

    @Column(name = "verification_token", nullable = false, unique = true)
    private String verificationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private DomainVerificationStatus verificationStatus = DomainVerificationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dns_records", columnDefinition = "jsonb")
    @Builder.Default
    private List<DnsRecord> dnsRecords = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "ssl_status", nullable = false, length = 50)
    @Builder.Default
    private SslStatus sslStatus = SslStatus.PENDING;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Business Logic Methods
    public void markAsVerified() {
        this.verificationStatus = DomainVerificationStatus.VERIFIED;
        this.verifiedAt = Instant.now();
    }

    public void markAsFailed() {
        this.verificationStatus = DomainVerificationStatus.FAILED;
    }

    public void activateSsl() {
        this.sslStatus = SslStatus.ACTIVE;
    }

    public void failSsl() {
        this.sslStatus = SslStatus.FAILED;
    }

    public boolean isVerified() {
        return this.verificationStatus == DomainVerificationStatus.VERIFIED;
    }

    public boolean isSslActive() {
        return this.sslStatus == SslStatus.ACTIVE;
    }
}