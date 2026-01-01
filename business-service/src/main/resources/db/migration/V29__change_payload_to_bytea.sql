-- ============================================================================
-- Change Outbox Payload from TEXT to BYTEA for Avro Binary Storage
-- ============================================================================

-- Change payload column type from TEXT to BYTEA
ALTER TABLE outbox_events 
ALTER COLUMN payload TYPE BYTEA USING payload::bytea;

-- Add comment
COMMENT ON COLUMN outbox_events.payload IS 'Avro binary serialized event payload';
