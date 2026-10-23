-- Persist AI profile on desktop agent tasks (Mission 10). Additive only.

ALTER TABLE agent_tasks
    ADD COLUMN ai_profile VARCHAR(32) NOT NULL DEFAULT 'FAST' AFTER goal;
