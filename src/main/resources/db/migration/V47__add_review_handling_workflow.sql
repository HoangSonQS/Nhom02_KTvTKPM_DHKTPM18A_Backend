ALTER TABLE cat_book_review
    ADD COLUMN order_id BIGINT,
    ADD COLUMN handling_status VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN issue_type VARCHAR(50),
    ADD COLUMN admin_public_reply TEXT,
    ADD COLUMN support_action VARCHAR(50),
    ADD COLUMN flagged_at TIMESTAMP,
    ADD COLUMN handled_by_user_id BIGINT,
    ADD COLUMN handled_at TIMESTAMP;

UPDATE cat_book_review
SET handling_status = 'NEEDS_ACTION',
    flagged_at = COALESCE(updated_at, created_at)
WHERE rating BETWEEN 1 AND 2;

CREATE INDEX IF NOT EXISTS idx_cat_book_review_handling_status ON cat_book_review(handling_status);
CREATE INDEX IF NOT EXISTS idx_cat_book_review_order_id ON cat_book_review(order_id);

CREATE TABLE IF NOT EXISTS cat_book_review_handling_history (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    issue_type VARCHAR(50),
    public_reply TEXT,
    support_action VARCHAR(50),
    note TEXT,
    handled_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cat_book_review_history_review FOREIGN KEY (review_id) REFERENCES cat_book_review(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cat_book_review_history_review_id ON cat_book_review_handling_history(review_id);
