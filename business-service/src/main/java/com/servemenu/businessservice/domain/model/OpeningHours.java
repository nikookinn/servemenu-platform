package com.servemenu.businessservice.domain.model;

import com.servemenu.businessservice.domain.enums.*;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "opening_hours",
        indexes = {
                @Index(name = "idx_opening_hours_store_id", columnList = "store_id"),
                @Index(name = "idx_opening_hours_store_day", columnList = "store_id, day_of_week")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_day", columnNames = {"store_id", "day_of_week"})
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpeningHours {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_opening_hours_store"))
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "time_slots", columnDefinition = "jsonb")
    @Builder.Default
    private List<TimeSlot> timeSlots = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addTimeSlot(LocalTime openTime, LocalTime closeTime) {
        TimeSlot slot = new TimeSlot(openTime, closeTime);
        this.timeSlots.add(slot);
    }

    public void removeTimeSlot(int index) {
        if (index >= 0 && index < timeSlots.size()) {
            this.timeSlots.remove(index);
        }
    }

    public void clearTimeSlots() {
        this.timeSlots.clear();
    }
}
