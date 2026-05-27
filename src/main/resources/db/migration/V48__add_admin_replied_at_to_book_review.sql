ALTER TABLE cat_book_review
    ADD COLUMN IF NOT EXISTS admin_replied_at TIMESTAMP;
