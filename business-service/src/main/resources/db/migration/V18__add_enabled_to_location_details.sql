-- Location details already has is_enabled field from V8 migration
-- This migration is a no-op but kept for consistency in versioning
-- The field is already present: is_enabled BOOLEAN NOT NULL DEFAULT FALSE

-- Verify the column exists and add comment for clarity
COMMENT ON COLUMN location_details.is_enabled 
    IS 'Toggle to enable/disable location-based features (QR scan validation, customer proximity check)';

-- Index already exists from V8 migration

