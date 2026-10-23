-- AI gateway usage and Ultra credit reservations (UTC day).
-- Does not wipe existing data.

ALTER TABLE user_settings
    ADD COLUMN preferred_ai_mode VARCHAR(32) NULL AFTER preferred_ai_profile;

UPDATE user_settings
SET preferred_ai_mode = preferred_ai_profile
WHERE preferred_ai_mode IS NULL;

CREATE TABLE IF NOT EXISTS ai_usage_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    feature         VARCHAR(64)  NOT NULL,
    mode            VARCHAR(32)  NOT NULL,
    provider        VARCHAR(64)  NOT NULL,
    model_id        VARCHAR(128) NOT NULL,
    request_id      VARCHAR(64)  NOT NULL,
    idempotency_key VARCHAR(128) NULL,
    prompt_tokens   INT          NOT NULL DEFAULT 0,
    completion_tokens INT        NOT NULL DEFAULT 0,
    latency_ms      INT          NOT NULL DEFAULT 0,
    ultra_credits   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL,
    error_category  VARCHAR(64)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_ai_usage_request_id (request_id),
    UNIQUE KEY uq_ai_usage_idempotency (user_id, idempotency_key),
    KEY idx_ai_usage_user_day (user_id, created_at),
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_quota_reservations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    utc_day         DATE         NOT NULL,
    feature         VARCHAR(64)  NOT NULL,
    mode            VARCHAR(32)  NOT NULL,
    credits         INT          NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_id      VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at      TIMESTAMP    NULL,
    UNIQUE KEY uq_ai_quota_idempotency (user_id, idempotency_key),
    KEY idx_ai_quota_user_day (user_id, utc_day, status),
    CONSTRAINT fk_ai_quota_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_provider_health (
    provider        VARCHAR(64)  NOT NULL PRIMARY KEY,
    status          VARCHAR(32)  NOT NULL,
    detail          VARCHAR(512) NULL,
    checked_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
