CREATE TABLE IF NOT EXISTS tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    table_name VARCHAR(100) NOT NULL,
    table_number VARCHAR(20) NOT NULL,
    qr_code_url VARCHAR(500),
    qr_code_id UUID,
    capacity INTEGER,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_table_store_id ON tables(store_id);
CREATE INDEX idx_table_qr_code_id ON tables(qr_code_id);
