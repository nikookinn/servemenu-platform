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
@Table(name = "order_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_order_settings_business"))
    private Business business;

    @Column(name = "enable_customer_tip", nullable = false)
    @Builder.Default
    private Boolean enableCustomerTip = true;

    @Column(name = "enable_cancel_order", nullable = false)
    @Builder.Default
    private Boolean enableCancelOrder = true;

    @Column(name = "invoice_id_prefix", nullable = false, length = 20)
    @Builder.Default
    private String invoiceIdPrefix = "INVOICE";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}