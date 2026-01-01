-- Create outbox_events table for Outbox Pattern
-- This table stores events before they are published to Kafka
-- Debezium CDC will read from this table and publish to Kafka automatically

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

-- Index for finding unpublished events (used by polling fallback)
CREATE INDEX idx_outbox_published ON outbox_events(published);

-- Index for cleanup job (delete old published events)
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

-- Index for Debezium CDC performance
CREATE INDEX idx_outbox_published_created ON outbox_events(published, created_at) WHERE published = FALSE;

-- Comments for documentation
COMMENT ON TABLE outbox_events IS 'Outbox pattern table for reliable event publishing via Debezium CDC';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate (e.g., mediaId)';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of aggregate (e.g., MediaAsset)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of event (e.g., MediaUploaded)';
COMMENT ON COLUMN outbox_events.topic IS 'Kafka topic to publish to';
COMMENT ON COLUMN outbox_events.payload IS 'JSON payload of the event';
COMMENT ON COLUMN outbox_events.published IS 'Whether event has been published to Kafka';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of retry attempts (for polling fallback)';
COMMENT ON COLUMN outbox_events.error_message IS 'Error message if publishing failed';
COMMENT ON COLUMN outbox_events.created_at IS 'When the event was created';
COMMENT ON COLUMN outbox_events.published_at IS 'When the event was published to Kafka';
