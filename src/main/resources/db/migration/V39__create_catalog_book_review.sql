CREATE TABLE IF NOT EXISTS cat_book_review (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reviewer_name VARCHAR(255),
    reviewer_email VARCHAR(255),
    rating INTEGER NOT NULL,
    content TEXT,
    edit_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cat_book_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_cat_book_review_edit_count CHECK (edit_count >= 0),
    CONSTRAINT uk_cat_book_review_book_user UNIQUE (book_id, user_id),
    CONSTRAINT fk_cat_book_review_book FOREIGN KEY (book_id) REFERENCES cat_book(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cat_book_review_book_id ON cat_book_review(book_id);
CREATE INDEX IF NOT EXISTS idx_cat_book_review_user_id ON cat_book_review(user_id);
CREATE INDEX IF NOT EXISTS idx_cat_book_review_created_at ON cat_book_review(created_at);
