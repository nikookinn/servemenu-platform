-- ============================================================================
-- User Audit Log Table
-- Tracks all user-related actions for compliance and debugging
-- ============================================================================

CREATE TABLE user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by UUID,
    changes JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_audit_log_user_timestamp ON user_audit_log(user_id, timestamp DESC);
CREATE INDEX idx_audit_log_action ON user_audit_log(action);
CREATE INDEX idx_audit_log_timestamp ON user_audit_log(timestamp DESC);
CREATE INDEX idx_audit_log_changed_by ON user_audit_log(changed_by);

-- GIN index for JSONB column (fast JSON queries)
CREATE INDEX idx_audit_log_changes ON user_audit_log USING GIN (changes);

-- Comments
COMMENT ON TABLE user_audit_log IS 'Audit trail for all user-related actions';
COMMENT ON COLUMN user_audit_log.action IS 'Type of action: CREATED, UPDATED, DELETED, LOGIN, etc.';
COMMENT ON COLUMN user_audit_log.changed_by IS 'User who performed the action';
COMMENT ON COLUMN user_audit_log.changes IS 'JSON object containing changed fields';
