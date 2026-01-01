-- Performance optimization indexes for gRPC batch operations
-- Improves query performance for IN clause operations

-- Composite index for batch ID lookups with soft delete check
-- This significantly improves performance of findAllByIdInAndDeletedFalse
CREATE INDEX idx_media_id_deleted ON media_assets(id) WHERE deleted_at IS NULL;

-- Covering index for common gRPC queries (includes frequently accessed columns)
-- This allows index-only scans without table access
CREATE INDEX idx_media_batch_lookup ON media_assets(id, entity_id, media_type, created_at) 
WHERE deleted_at IS NULL;

-- Comment for documentation
COMMENT ON INDEX idx_media_id_deleted IS 'Optimizes batch ID lookups in gRPC operations with soft delete filtering';
COMMENT ON INDEX idx_media_batch_lookup IS 'Covering index for gRPC batch operations - enables index-only scans';
