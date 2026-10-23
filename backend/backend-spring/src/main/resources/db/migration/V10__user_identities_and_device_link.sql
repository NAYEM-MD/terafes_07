-- ==============================================================================
-- TRON AI — Migration V10: Google User Identities and Device Link Extensions
-- ==============================================================================

-- ── 1. User Identities (Google OAuth Provider Subject Mapping) ────────────────
CREATE TABLE IF NOT EXISTS user_identities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email_at_login VARCHAR(255) NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_identity_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_user_identities_user_id ON user_identities(user_id);
CREATE INDEX idx_user_identities_provider_sub ON user_identities(provider, provider_subject);

-- ── 2. Device Link Request Extensions ─────────────────────────────────────────
ALTER TABLE desktop_pairing_requests
    ADD COLUMN requested_workspace VARCHAR(1024) NULL;

ALTER TABLE desktop_pairing_requests
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

-- ── 3. Desktop Device Access (Optional Shared Device Access & Multi-Tenant Support) ──
CREATE TABLE IF NOT EXISTS desktop_device_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'OWNER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_desktop_device_access_device FOREIGN KEY (device_id) REFERENCES desktop_devices(id) ON DELETE CASCADE,
    CONSTRAINT fk_desktop_device_access_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_desktop_device_access_dev_user UNIQUE (device_id, user_id)
);

CREATE INDEX idx_desktop_device_access_device ON desktop_device_access(device_id);
CREATE INDEX idx_desktop_device_access_user ON desktop_device_access(user_id);
