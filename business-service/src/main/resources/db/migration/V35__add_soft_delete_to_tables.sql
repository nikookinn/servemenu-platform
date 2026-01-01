-- Add soft delete columns to tables table
-- This enables soft delete functionality: tables are marked as deleted instead of being removed from database
-- Benefits: data recovery, audit trail, analytics, compliance

-- Add deleted_at timestamp column
ALTER TABLE tables
ADD COLUMN deleted_at TIMESTAMP;

-- Add deleted_by UUID column (references user who deleted the table)
ALTER TABLE tables
ADD COLUMN deleted_by UUID;

-- Add index on deleted_at for efficient filtering of active tables
-- Most queries will filter WHERE deleted_at IS NULL
CREATE INDEX idx_tables_deleted_at ON tables(deleted_at);

-- Add composite index for store queries with soft delete
-- Optimizes: SELECT * FROM tables WHERE store_id = ? AND deleted_at IS NULL
CREATE INDEX idx_tables_store_id_deleted_at ON tables(store_id, deleted_at);

-- Add comment for documentation
COMMENT ON COLUMN tables.deleted_at IS 'Timestamp when table was soft deleted (NULL = active)';
COMMENT ON COLUMN tables.deleted_by IS 'UUID of user who deleted the table';
