CREATE TABLE telegram_send_log (
    id            BIGSERIAL    PRIMARY KEY,
    telegram_id   VARCHAR(64)  NOT NULL,
    message_text  TEXT         NOT NULL,
    http_status   SMALLINT,                      -- NULL = 예외로 HTTP 응답 없음
    error_message VARCHAR(512),
    bot_blocked   BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tsl_telegram_id ON telegram_send_log (telegram_id);
CREATE INDEX idx_tsl_sent_at     ON telegram_send_log (sent_at DESC);
CREATE INDEX idx_tsl_bot_blocked ON telegram_send_log (telegram_id) WHERE bot_blocked = TRUE;
