CREATE TABLE IF NOT EXISTS business_audit_log (
    id BIGSERIAL PRIMARY KEY,
    business_id UUID NOT NULL,
    store_id UUID,
    performed_by UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    changes JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_audit_log_business_id ON business_audit_log(business_id);
CREATE INDEX idx_audit_log_store_id ON business_audit_log(store_id);
CREATE INDEX idx_audit_log_performed_by ON business_audit_log(performed_by);
CREATE INDEX idx_audit_log_timestamp ON business_audit_log(timestamp);
CREATE INDEX idx_audit_log_business_timestamp ON business_audit_log(business_id, timestamp);