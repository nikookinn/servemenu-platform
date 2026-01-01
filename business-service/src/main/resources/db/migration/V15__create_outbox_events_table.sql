CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN DEFAULT FALSE NOT NULL,
    retry_count INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
    );

CREATE INDEX idx_outbox_processed_created ON outbox_events(processed, created_at);
CREATE INDEX idx_outbox_event_type ON outbox_events(event_type);