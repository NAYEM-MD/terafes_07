-- Multi-turn task messages and lightweight managed-browser metadata.
-- Browser cookies and credentials stay on the desktop. This table stores no secrets.

ALTER TABLE agent_tasks ADD COLUMN turn_step_index INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS agent_task_messages (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_task_messages_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_task_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_task_messages_task ON agent_task_messages(task_id, created_at);

CREATE TABLE IF NOT EXISTS agent_browser_sessions (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    device_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    state VARCHAR(30) NOT NULL,
    current_url VARCHAR(2048) NULL,
    title VARCHAR(500) NULL,
    profile_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NULL,
    CONSTRAINT fk_browser_session_task FOREIGN KEY (task_id) REFERENCES agent_tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_browser_session_device FOREIGN KEY (device_id) REFERENCES desktop_devices(id) ON DELETE CASCADE
);
CREATE INDEX idx_browser_session_task ON agent_browser_sessions(task_id);
