CREATE TABLE IF NOT EXISTS custom_domains (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE REFERENCES businesses(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    verification_token VARCHAR(255) NOT NULL UNIQUE,
    verification_status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    dns_records JSONB,
    ssl_status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_custom_domain_business_id ON custom_domains(business_id);
CREATE INDEX idx_custom_domain_domain ON custom_domains(domain);