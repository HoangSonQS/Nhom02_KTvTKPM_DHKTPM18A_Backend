ALTER TABLE ai_agent_pending_action
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

UPDATE ai_agent_pending_action
SET status = CASE
    WHEN confirmed_at IS NOT NULL THEN 'CONFIRMED'
    WHEN cancelled_at IS NOT NULL THEN 'CANCELLED'
    WHEN expires_at < CURRENT_TIMESTAMP THEN 'EXPIRED'
    ELSE 'PENDING'
END
WHERE status IS NULL OR status = 'PENDING';
