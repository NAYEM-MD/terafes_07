-- ==============================================================================
-- TRON AI — Migration V4: Universal AI Performance Profiles & Session Locking
-- Fast / Quality Model Routing Foundation
-- ==============================================================================

-- ── 1. Chat Sessions Profile Support ──────────────────────────────────────────
ALTER TABLE chat_sessions ADD COLUMN ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST';

-- ── 2. Interview Sessions Profile & Model Trace ───────────────────────────────
ALTER TABLE interview_sessions ADD COLUMN ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST';
ALTER TABLE interview_sessions ADD COLUMN model_id VARCHAR(100) NULL;

-- ── 3. Agent Runs Profile Support ─────────────────────────────────────────────
ALTER TABLE agent_runs ADD COLUMN ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST';

-- ── 4. Voice Sessions Durable Foundation (Ephemeral Audio, Durable Lock) ─────
CREATE TABLE IF NOT EXISTS voice_sessions (
    id VARCHAR(100) PRIMARY KEY,
    user_id BIGINT NULL,
    ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST',
    model_id VARCHAR(100) NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_voice_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_voice_sessions_user ON voice_sessions(user_id);

-- ── 5. User Preferred Profile ────────────────────────────────────────────────
ALTER TABLE user_settings ADD COLUMN preferred_ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST';
