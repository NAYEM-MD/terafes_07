-- ==============================================================================
-- TRON AI — Migration V6: Desktop Agent control plane
-- Devices, pairing, tasks, approvals, file-change metadata
-- ==============================================================================

CREATE TABLE IF NOT EXISTS desktop_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_uuid VARCHAR(36) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    os_name VARCHAR(100) NULL,
    hostname VARCHAR(255) NULL,
    agent_version VARCHAR(50) NULL,
    public_key TEXT NULL,
    fingerprint VARCHAR(128) NULL,
    credential_hash VARCHAR(64) NULL,
    approval_key_envelope TEXT NULL,
    status VARCHAR(30) NOT NULL,
    last_seen TIMESTAMP NULL,
    paired_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    allowed_workspace VARCHAR(1024) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_desktop_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_desktop_device_uuid UNIQUE (device_uuid)
);
CREATE INDEX idx_desktop_devices_user ON desktop_devices(user_id);
CREATE INDEX idx_desktop_devices_credential ON desktop_devices(credential_hash);

CREATE TABLE IF NOT EXISTS desktop_pairing_requests (
    id VARCHAR(36) PRIMARY KEY,
    code_hash VARCHAR(64) NOT NULL,
    poll_secret_hash VARCHAR(64) NOT NULL,
    device_uuid VARCHAR(36) NOT NULL,
    public_key TEXT NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    os_name VARCHAR(100) NULL,
    hostname VARCHAR(255) NULL,
    agent_version VARCHAR(50) NULL,
    display_name VARCHAR(255) NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    credential_delivered_at TIMESTAMP NULL,
    credential_envelope TEXT NULL,
    approval_key_envelope TEXT NULL,
    device_id BIGINT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pairing_code_hash ON desktop_pairing_requests(code_hash);
CREATE INDEX idx_pairing_poll_hash ON desktop_pairing_requests(poll_secret_hash);

CREATE TABLE IF NOT EXISTS agent_tasks (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    goal VARCHAR(4000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    control_mode VARCHAR(30) NOT NULL DEFAULT 'AGENT',
    summary LONGTEXT NULL,
    workspace VARCHAR(1024) NOT NULL,
    action_count INT NOT NULL DEFAULT 0,
    needs_reinspect BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    CONSTRAINT fk_agent_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_tasks_device FOREIGN KEY (device_id) REFERENCES desktop_devices(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_tasks_user ON agent_tasks(user_id);
CREATE INDEX idx_agent_tasks_device ON agent_tasks(device_id);

CREATE TABLE IF NOT EXISTS agent_task_steps (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    step_index INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    tool VARCHAR(50) NULL,
    operation VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_task_steps_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_task_steps_task ON agent_task_steps(task_id);

CREATE TABLE IF NOT EXISTS agent_action_executions (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    action_id VARCHAR(36) NOT NULL,
    tool VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    risk VARCHAR(30) NOT NULL,
    working_directory VARCHAR(1024) NULL,
    arguments_json LONGTEXT NULL,
    stdout_text LONGTEXT NULL,
    stderr_text LONGTEXT NULL,
    exit_code INT NULL,
    duration_ms BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_exec_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_exec_task ON agent_action_executions(task_id);

CREATE TABLE IF NOT EXISTS agent_approvals (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    action_id VARCHAR(36) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    risk VARCHAR(30) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    command_preview VARCHAR(2000) NULL,
    path_preview VARCHAR(1024) NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_approvals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_approvals_device FOREIGN KEY (device_id) REFERENCES desktop_devices(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_approvals_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_approvals_task ON agent_approvals(task_id);

CREATE TABLE IF NOT EXISTS agent_file_changes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    action_id VARCHAR(36) NULL,
    path VARCHAR(1024) NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    sha256 VARCHAR(64) NULL,
    size_bytes BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_file_changes_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_file_changes_task ON agent_file_changes(task_id);
