ALTER TABLE ntf_notification_log
    ADD COLUMN recipient_user_id BIGINT,
    ADD COLUMN title VARCHAR(255),
    ADD COLUMN message TEXT,
    ADD COLUMN read_at TIMESTAMPTZ;

CREATE INDEX idx_ntf_recipient_created_at ON ntf_notification_log(recipient_user_id, created_at DESC);
CREATE INDEX idx_ntf_recipient_unread ON ntf_notification_log(recipient_user_id) WHERE read_at IS NULL;
