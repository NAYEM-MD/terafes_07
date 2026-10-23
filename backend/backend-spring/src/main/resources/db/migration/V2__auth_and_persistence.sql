-- ==============================================================================
-- TRON AI — Migration V2: Identity, Auth, Persistence, CV, Interview, Chat
-- Step 2: Multi-User Durable Foundation
-- ==============================================================================

-- ── 1. Evolution of users table ──────────────────────────────────────────────
ALTER TABLE users ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;

-- ── 2. Refresh Tokens (Phase E) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ── 3. Chat Sessions & Messages (Phase L) ────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_session_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'New Conversation',
    model_id VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_session_user_client UNIQUE (user_id, client_session_id)
);
CREATE INDEX idx_chat_sessions_user ON chat_sessions(user_id);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    content LONGTEXT NOT NULL,
    language VARCHAR(30) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);

-- ── 4. CV Documents & Profiles (Phase O) ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS cv_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    page_count INT NOT NULL DEFAULT 1,
    text_characters INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cv_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_cv_documents_user ON cv_documents(user_id);

CREATE TABLE IF NOT EXISTS cv_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cv_document_id BIGINT NOT NULL UNIQUE,
    education_json LONGTEXT NULL,
    skills_json LONGTEXT NULL,
    work_experience_json LONGTEXT NULL,
    extraction_notes_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cv_profiles_doc FOREIGN KEY (cv_document_id) REFERENCES cv_documents(id) ON DELETE CASCADE
);

-- ── 5. Interview Sessions, Messages, Analysis (Phase T) ──────────────────────
CREATE TABLE IF NOT EXISTS interview_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    config_json LONGTEXT NOT NULL,
    current_question_index INT NOT NULL DEFAULT 0,
    question_count INT NOT NULL DEFAULT 5,
    finished BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_interview_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_sessions_user ON interview_sessions(user_id);

CREATE TABLE IF NOT EXISTS interview_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(30) NOT NULL,
    question_index INT NOT NULL,
    message_type VARCHAR(50) NOT NULL DEFAULT 'text',
    text LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_messages_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
);
CREATE INDEX idx_interview_messages_session ON interview_messages(session_id);

CREATE TABLE IF NOT EXISTS interview_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    overall_score INT NOT NULL DEFAULT 0,
    communication_score INT NOT NULL DEFAULT 0,
    japanese_score INT NOT NULL DEFAULT 0,
    technical_score INT NOT NULL DEFAULT 0,
    confidence_score INT NOT NULL DEFAULT 0,
    radar_json LONGTEXT NULL,
    strong_points_json LONGTEXT NULL,
    weak_points_json LONGTEXT NULL,
    feedback LONGTEXT NULL,
    star_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_analysis_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
);
