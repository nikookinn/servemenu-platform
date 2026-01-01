package com.servemenu.businessservice.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_processed_created", columnList = "processed, created_at"),
                @Index(name = "idx_outbox_event_type", columnList = "event_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", length = 100)
    private String aggregateType;

    @Column(name = "payload", nullable = false, columnDefinition = "BYTEA")
    private byte[] payload;
    
    @Column(name = "trace_id", length = 200)
    private String traceId;
    
    @Column(name = "correlation_id", length = 200)
    private String correlationId;
    
    @Column(name = "schema_version")
    @Builder.Default
    private Integer schemaVersion = 1;
    
    @Column(name = "source", length = 200)
    private String source;
    
    @Column(name = "timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }
    
    public void markAsPublished() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
