-- Add 2025 enterprise standard fields to outbox_events table
-- These fields enable distributed tracing, correlation, schema evolution, and source tracking

-- Add missing fields for 2025 standards
ALTER TABLE outbox_events 
ADD COLUMN IF NOT EXISTS trace_id VARCHAR(200),
ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(200);

-- Add schema versioning for event evolution
ALTER TABLE outbox_events 
ADD COLUMN IF NOT EXISTS schema_version INT DEFAULT 1;

-- Add source field to track event origin
ALTER TABLE outbox_events 
ADD COLUMN IF NOT EXISTS source VARCHAR(200);

-- Add timestamp field if not exists (for better time tracking)
ALTER TABLE outbox_events
ADD COLUMN IF NOT EXISTS timestamp TIMESTAMPTZ DEFAULT NOW();

-- Rename 'published' to 'processed' for consistency
ALTER TABLE outbox_events 
RENAME COLUMN published TO processed;

ALTER TABLE outbox_events 
RENAME COLUMN published_at TO processed_at;

-- Update existing records with default values
UPDATE outbox_events 
SET aggregate_type = CASE 
    WHEN event_type LIKE '%Media%' THEN 'media'
    WHEN event_type LIKE '%QR%' THEN 'qr'
    ELSE 'media'
END
WHERE aggregate_type IS NULL;

UPDATE outbox_events 
SET source = 'media-service'
WHERE source IS NULL;

UPDATE outbox_events 
SET schema_version = 1
WHERE schema_version IS NULL;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_outbox_trace_id ON outbox_events(trace_id);
CREATE INDEX IF NOT EXISTS idx_outbox_correlation_id ON outbox_events(correlation_id);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_type ON outbox_events(aggregate_type);
CREATE INDEX IF NOT EXISTS idx_outbox_timestamp ON outbox_events(timestamp);

-- Add composite index for Debezium CDC performance
CREATE INDEX IF NOT EXISTS idx_outbox_cdc ON outbox_events(processed, timestamp) WHERE processed = FALSE;

-- Add comments for documentation
COMMENT ON COLUMN outbox_events.trace_id IS '2025 Standard: Distributed tracing ID (Zipkin/Jaeger)';
COMMENT ON COLUMN outbox_events.correlation_id IS '2025 Standard: Request correlation ID for tracking';
COMMENT ON COLUMN outbox_events.schema_version IS '2025 Standard: Event schema version for evolution';
COMMENT ON COLUMN outbox_events.source IS '2025 Standard: Source service that generated the event';
COMMENT ON COLUMN outbox_events.timestamp IS '2025 Standard: Event timestamp with timezone';
