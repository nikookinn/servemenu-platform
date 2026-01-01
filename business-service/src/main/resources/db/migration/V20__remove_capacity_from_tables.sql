-- Remove capacity column from tables
-- QR code generation will be handled by dedicated QR service in the future
ALTER TABLE tables DROP COLUMN IF EXISTS capacity;
