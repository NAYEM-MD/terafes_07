-- ==============================================================================
-- TRON AI — Migration V3: Agentic Interview Preparation Framework
-- Mega Mission 1.5: Multi-Step Autonomous Agent Runs, Steps, and Results
-- ==============================================================================

-- ── 1. Agent Runs Table ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_runs (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal TEXT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    job_position VARCHAR(255) NOT NULL,
    job_description LONGTEXT NOT NULL,
    language VARCHAR(30) NOT NULL DEFAULT 'ja',
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    steps_executed INT NOT NULL DEFAULT 0,
    model_id VARCHAR(100) NOT NULL DEFAULT 'qwen3-8b',
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_runs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_runs_user_id ON agent_runs(user_id);

-- ── 2. Agent Steps Table (Execution Trace) ────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    step_number INT NOT NULL,
    action VARCHAR(60) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_steps_run FOREIGN KEY (run_id) REFERENCES agent_runs(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_steps_run_id ON agent_steps(run_id);

-- ── 3. Agent Results Table (Derived Preparation Package) ──────────────────────
CREATE TABLE IF NOT EXISTS agent_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL UNIQUE,
    result_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_results_run FOREIGN KEY (run_id) REFERENCES agent_runs(id) ON DELETE CASCADE
);
CREATE INDEX idx_agent_results_run_id ON agent_results(run_id);
