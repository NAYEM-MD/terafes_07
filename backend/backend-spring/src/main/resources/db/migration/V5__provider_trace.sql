-- Provider trace for Chat, Interview, and Agent.
-- logical profile stays in ai_profile. provider_type is LOCAL or CLOUD.
-- actual_model_id is the model that served the request.

ALTER TABLE chat_sessions ADD COLUMN provider_type VARCHAR(16) NULL;
ALTER TABLE chat_sessions ADD COLUMN actual_model_id VARCHAR(200) NULL;

ALTER TABLE interview_sessions ADD COLUMN provider_type VARCHAR(16) NULL;
ALTER TABLE interview_sessions ADD COLUMN actual_model_id VARCHAR(200) NULL;

ALTER TABLE agent_runs ADD COLUMN provider_type VARCHAR(16) NULL;
ALTER TABLE agent_runs ADD COLUMN actual_model_id VARCHAR(200) NULL;
