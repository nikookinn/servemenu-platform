package com.servemenu.userservice.domain.model;

import com.servemenu.userservice.domain.enums.Theme;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_preferences",
        indexes = {
                @Index(name = "idx_user_preferences_user_id", columnList = "user_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_user_preferences_user"))
    private User user;

    @Column(name = "dashboard_language", length = 5, nullable = false)
    private String dashboardLanguage = "en";

    @Column(name = "timezone", length = 50, nullable = false)
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 20, nullable = false)
    private Theme theme = Theme.LIGHT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserPreferences() {
    }

    public UserPreferences(
            UUID id,
            User user,
            String dashboardLanguage,
            String timezone,
            Theme theme,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.user = user;
        this.dashboardLanguage = dashboardLanguage;
        this.timezone = timezone;
        this.theme = theme;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Business methods
    public void changeDashboardLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }
        this.dashboardLanguage = language;
    }

    public void changeTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Timezone cannot be null or empty");
        }
        
        // Validate timezone
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
        
        this.timezone = timezone;
    }

    public void changeTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme cannot be null");
        }
        this.theme = theme;
    }

    public void updatePreferences(String language, String timezone, Theme theme) {
        if (language != null) {
            changeDashboardLanguage(language);
        }
        if (timezone != null) {
            changeTimezone(timezone);
        }
        if (theme != null) {
            changeTheme(theme);
        }
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

    public String getDashboardLanguage() {
        return dashboardLanguage;
    }

    public void setDashboardLanguage(String dashboardLanguage) {
        this.dashboardLanguage = dashboardLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
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
        if (!(o instanceof UserPreferences that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}