-- TRON V4-C: bind agent-engine sessions to paired desktop devices

ALTER TABLE agent_engine_sessions
    ADD COLUMN device_id BIGINT NULL;

CREATE INDEX idx_agent_engine_sessions_device ON agent_engine_sessions(device_id);
