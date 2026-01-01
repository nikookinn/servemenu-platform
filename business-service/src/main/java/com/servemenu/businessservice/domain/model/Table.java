package com.servemenu.businessservice.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(
        name = "tables",
        indexes = {
                @Index(name = "idx_table_store_id", columnList = "store_id"),
                @Index(name = "idx_table_qr_code_media_id", columnList = "qr_code_media_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_table_store"))
    private Store store;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "table_number", nullable = false)
    private String tableNumber;

    // Media ID (stored in database - reference to Media Service)
    @Column(name = "qr_code_media_id")
    private UUID qrCodeMediaId;

    // Transient field (not persisted - enriched at runtime via gRPC)
    @Transient
    private String qrCodeUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Soft delete fields
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // Business Logic Methods
    public void updateName(String newName) {
        this.tableName = newName;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void assignQRCode(UUID qrCodeMediaId) {
        this.qrCodeMediaId = qrCodeMediaId;
    }

    public void softDelete(UUID deletedBy) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
        this.isActive = false;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}