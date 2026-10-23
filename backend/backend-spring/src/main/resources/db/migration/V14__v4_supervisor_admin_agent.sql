-- TRON V4: AI Supervisor plans, hybrid control plane, agent engine sessions, admin audit

CREATE TABLE IF NOT EXISTS supervisor_plans (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_goal LONGTEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    engine VARCHAR(40) NOT NULL,
    model_route VARCHAR(40) NOT NULL,
    current_milestone_index INT NOT NULL DEFAULT 0,
    plan_json LONGTEXT NULL,
    verified_result LONGTEXT NULL,
    error_summary LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supervisor_plans_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_supervisor_plans_user ON supervisor_plans(user_id);
CREATE INDEX idx_supervisor_plans_status ON supervisor_plans(status);

CREATE TABLE IF NOT EXISTS supervisor_milestones (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL,
    milestone_index INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    acceptance_criteria LONGTEXT NULL,
    result_summary LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supervisor_milestones_plan FOREIGN KEY (plan_id) REFERENCES supervisor_plans(id) ON DELETE CASCADE,
    CONSTRAINT uq_supervisor_milestone UNIQUE (plan_id, milestone_index)
);

CREATE TABLE IF NOT EXISTS supervisor_events (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    detail LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supervisor_events_plan FOREIGN KEY (plan_id) REFERENCES supervisor_plans(id) ON DELETE CASCADE
);
CREATE INDEX idx_supervisor_events_plan ON supervisor_events(plan_id);

CREATE TABLE IF NOT EXISTS agent_engine_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id VARCHAR(36) NULL,
    engine_type VARCHAR(40) NOT NULL,
    external_session_id VARCHAR(200) NULL,
    status VARCHAR(40) NOT NULL,
    workspace_path VARCHAR(1000) NULL,
    goal LONGTEXT NULL,
    last_error LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_engine_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_engine_sessions_user ON agent_engine_sessions(user_id);

CREATE TABLE IF NOT EXISTS platform_control_state (
    id INT PRIMARY KEY,
    mode VARCHAR(40) NOT NULL,
    local_routing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cloud_routing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    pause_new_ai_tasks BOOLEAN NOT NULL DEFAULT FALSE,
    stop_new_agent_jobs BOOLEAN NOT NULL DEFAULT FALSE,
    exhibition_registration_open BOOLEAN NOT NULL DEFAULT TRUE,
    max_concurrent_ai_requests INT NOT NULL DEFAULT 10,
    queue_capacity INT NOT NULL DEFAULT 50,
    rtx_override VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    gtx_override VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL
);

INSERT INTO platform_control_state (
    id, mode, local_routing_enabled, cloud_routing_enabled,
    pause_new_ai_tasks, stop_new_agent_jobs, exhibition_registration_open,
    max_concurrent_ai_requests, queue_capacity, rtx_override, gtx_override
) VALUES (
    1, 'NORMAL', TRUE, TRUE, FALSE, FALSE, TRUE, 10, 50, 'AUTO', 'AUTO'
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NULL,
    target_id VARCHAR(120) NULL,
    detail LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_admin_audit_created ON admin_audit_logs(created_at);

ALTER TABLE users ADD COLUMN suspended_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN suspend_reason VARCHAR(500) NULL;
