-- V21: Init System Audit Logs table for Phase 9 Hardening
CREATE TABLE sys_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    target TEXT,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON sys_audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON sys_audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON sys_audit_logs(created_at);
