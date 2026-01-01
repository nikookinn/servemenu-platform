package com.servemenu.mediaservice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Pattern Entity for reliable event publishing
 * Events are stored in DB first, then published to Kafka by Debezium CDC
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_published", columnList = "processed"),
        @Index(name = "idx_outbox_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "topic", nullable = false)
    private String topic;

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

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
