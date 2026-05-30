CREATE TABLE IF NOT EXISTS ai_agent_pending_action (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    intent VARCHAR(32) NOT NULL,
    payload TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_agent_pending_action_user
    ON ai_agent_pending_action(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_agent_pending_action_session
    ON ai_agent_pending_action(session_id);
