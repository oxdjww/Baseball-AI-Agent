CREATE TABLE notification_history (
    id                BIGSERIAL    PRIMARY KEY,
    game_id           VARCHAR(64)  NOT NULL,
    notification_type VARCHAR(32)  NOT NULL,
    sent_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_history_game_type
        UNIQUE (game_id, notification_type)
);

CREATE INDEX idx_nh_sent_at ON notification_history (sent_at DESC);
CREATE INDEX idx_nh_game_id ON notification_history (game_id);
