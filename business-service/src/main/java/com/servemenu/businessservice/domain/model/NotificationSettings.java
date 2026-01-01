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
@Table(name = "notification_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_notification_settings_business"))
    private Business business;

    @Column(name = "order_notification_sound", length = 100)
    private String orderNotificationSound;

    @Column(name = "order_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean orderNotificationEnabled = true;

    @Column(name = "feedback_notification_sound", length = 100)
    private String feedbackNotificationSound;

    @Column(name = "feedback_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean feedbackNotificationEnabled = true;

    @Column(name = "hot_action_notification_sound", length = 100)
    private String hotActionNotificationSound;

    @Column(name = "hot_action_notification_enabled", nullable = false)
    @Builder.Default
    private Boolean hotActionNotificationEnabled = true;

    @Column(name = "allow_order_email_notifications", nullable = false)
    @Builder.Default
    private Boolean allowOrderEmailNotifications = false;

    @Column(name = "order_notification_emails", columnDefinition = "TEXT")
    private String orderNotificationEmails;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Business methods
    public void enableEmailNotifications(String emails) {
        if (emails == null || emails.isBlank()) {
            throw new IllegalArgumentException("At least one email is required");
        }
        this.allowOrderEmailNotifications = true;
        this.orderNotificationEmails = emails;
    }

    public void disableEmailNotifications() {
        this.allowOrderEmailNotifications = false;
    }

    public void updateEmailAddresses(String emails) {
        if (emails == null || emails.isBlank()) {
            throw new IllegalArgumentException("Email addresses cannot be empty");
        }
        this.orderNotificationEmails = emails;
    }

    public java.util.List<String> getEmailList() {
        if (orderNotificationEmails == null || orderNotificationEmails.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(orderNotificationEmails.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}