CREATE TABLE IF NOT EXISTS system_settings (
    feature_key VARCHAR(100) PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO system_settings (feature_key, enabled) VALUES
    ('AI_ANALYSIS',        FALSE),
    ('REVERSAL_DETECTION', TRUE),
    ('RAIN_ALERT',         TRUE)
ON CONFLICT (feature_key) DO NOTHING;
