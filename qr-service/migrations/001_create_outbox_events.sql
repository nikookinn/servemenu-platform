-- Outbox Pattern table for QR Service
-- Debezium CDC reads from this table and publishes to Kafka

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,  -- Base64-encoded Avro binary
    published BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

-- Index for Debezium CDC performance
CREATE INDEX idx_outbox_published_created ON outbox_events(published, created_at) WHERE published = FALSE;

-- Index for cleanup job
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

-- Comments for documentation
COMMENT ON TABLE outbox_events IS 'Outbox pattern table for Debezium CDC event publishing';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate (e.g., tableId)';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of aggregate (e.g., Table)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of event (e.g., QRGenerated)';
COMMENT ON COLUMN outbox_events.topic IS 'Kafka topic to publish to';
COMMENT ON COLUMN outbox_events.payload IS 'Base64-encoded Avro binary';
COMMENT ON COLUMN outbox_events.published IS 'Whether event has been published to Kafka by Debezium';
COMMENT ON COLUMN outbox_events.created_at IS 'When the event was created';
COMMENT ON COLUMN outbox_events.published_at IS 'When Debezium published the event';
