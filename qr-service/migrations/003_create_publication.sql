-- ============================================================================
-- Create PostgreSQL Publication for Debezium CDC
-- This allows Debezium to capture changes from outbox_events table
-- ============================================================================

-- Create publication for outbox_events table
-- Note: This requires REPLICATION privilege (granted in init.sh)
DO $$
BEGIN
    -- Check if publication already exists
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication WHERE pubname = 'qr_outbox_publication'
    ) THEN
        CREATE PUBLICATION qr_outbox_publication FOR TABLE outbox_events;
        RAISE NOTICE '✅ Publication created: qr_outbox_publication';
    ELSE
        RAISE NOTICE '⏭️  Publication already exists: qr_outbox_publication';
    END IF;
END $$;

