CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_owner_id UUID NOT NULL,
    keycloak_user_id UUID NOT NULL UNIQUE,
    business_name VARCHAR(255),
    business_type VARCHAR(50),
    currency VARCHAR(3),
    supported_languages JSONB,
    default_language VARCHAR(5),
    slug VARCHAR(255) UNIQUE,
    custom_domain VARCHAR(255) UNIQUE,
    custom_domain_verified BOOLEAN DEFAULT FALSE,
    is_onboarding_completed BOOLEAN DEFAULT FALSE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
    );

CREATE INDEX idx_business_owner_id ON businesses(business_owner_id);
CREATE INDEX idx_business_keycloak_user_id ON businesses(keycloak_user_id);
CREATE INDEX idx_business_slug ON businesses(slug);
CREATE INDEX idx_business_custom_domain ON businesses(custom_domain);
CREATE INDEX idx_business_owner_status ON businesses(business_owner_id, status);