CREATE TABLE IF NOT EXISTS stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    store_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    address VARCHAR(500),
    email VARCHAR(255),
    phone_number VARCHAR(20),
    country_code VARCHAR(5),
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    menu_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
    );

CREATE INDEX idx_store_business_id ON stores(business_id);
CREATE INDEX idx_store_business_status ON stores(business_id, status);
CREATE INDEX idx_store_is_default ON stores(is_default);