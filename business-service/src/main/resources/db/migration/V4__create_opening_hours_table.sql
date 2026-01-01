CREATE TABLE IF NOT EXISTS opening_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    is_open BOOLEAN DEFAULT TRUE NOT NULL,
    time_slots JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_day UNIQUE (store_id, day_of_week)
    );

CREATE INDEX idx_opening_hours_store_id ON opening_hours(store_id);
CREATE INDEX idx_opening_hours_store_day ON opening_hours(store_id, day_of_week);