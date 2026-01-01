-- ============================================================================
-- Outbox Events Table - 2025 Enterprise Standard
-- Supports: Debezium CDC, Distributed Tracing, Schema Evolution
-- ============================================================================

CREATE TABLE outbox_events (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event Identification (2025 Standard)
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    
    -- Kafka Configuration
    topic VARCHAR(255) NOT NULL,
    
    -- Event Payload
    payload TEXT NOT NULL,
    
    -- Processing Status
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    
    -- Distributed Tracing (2025 Standard)
    trace_id VARCHAR(200),
    correlation_id VARCHAR(200),
    
    -- Schema Evolution (2025 Standard)
    schema_version INT DEFAULT 1,
    
    -- Source Tracking (2025 Standard)
    source VARCHAR(200) DEFAULT 'user-service',
    
    -- Timestamps
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Error Handling
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

-- Performance Indexes
CREATE INDEX idx_outbox_processed ON outbox_events(processed, created_at);
CREATE INDEX idx_outbox_event_type ON outbox_events(event_type);
CREATE INDEX idx_outbox_aggregate_type ON outbox_events(aggregate_type);
CREATE INDEX idx_outbox_retry ON outbox_events(retry_count) WHERE processed = FALSE;

-- Distributed Tracing Indexes (2025 Standard)
CREATE INDEX idx_outbox_trace_id ON outbox_events(trace_id);
CREATE INDEX idx_outbox_correlation_id ON outbox_events(correlation_id);
CREATE INDEX idx_outbox_timestamp ON outbox_events(timestamp);

-- Debezium CDC Optimization Index (2025 Standard)
CREATE INDEX idx_outbox_cdc ON outbox_events(processed, timestamp) WHERE processed = FALSE;

-- Documentation Comments
COMMENT ON TABLE outbox_events IS '2025 Standard: Outbox pattern for transactional event publishing with Debezium CDC';
COMMENT ON COLUMN outbox_events.aggregate_type IS '2025 Standard: Type of aggregate (user, business, store, etc.) for event routing';
COMMENT ON COLUMN outbox_events.event_type IS '2025 Standard: Type of domain event (PascalCase)';
COMMENT ON COLUMN outbox_events.topic IS 'Kafka topic to publish to (format: events.{aggregate}.{type})';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate that generated the event';
COMMENT ON COLUMN outbox_events.processed IS 'Whether event has been successfully processed by Debezium';
COMMENT ON COLUMN outbox_events.trace_id IS '2025 Standard: Distributed tracing ID (Zipkin/Jaeger)';
COMMENT ON COLUMN outbox_events.correlation_id IS '2025 Standard: Request correlation ID for tracking';
COMMENT ON COLUMN outbox_events.schema_version IS '2025 Standard: Event schema version for evolution';
COMMENT ON COLUMN outbox_events.source IS '2025 Standard: Source service that generated the event';
COMMENT ON COLUMN outbox_events.timestamp IS '2025 Standard: Event timestamp with timezone';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of publish retry attempts';
