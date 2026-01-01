-- Add error_message column to outbox_events table for better error tracking
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS error_message TEXT;
