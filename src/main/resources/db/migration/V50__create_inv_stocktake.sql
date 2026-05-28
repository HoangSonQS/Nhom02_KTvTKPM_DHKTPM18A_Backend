CREATE TABLE IF NOT EXISTS inv_stocktake_session (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_staff_id BIGINT,
    assigned_staff_email VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_by VARCHAR(255),
    submitted_at TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    rejected_by VARCHAR(255),
    rejected_at TIMESTAMP,
    reject_reason TEXT,
    cancelled_by VARCHAR(255),
    cancelled_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inv_stocktake_item (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    system_quantity INTEGER NOT NULL,
    actual_quantity INTEGER,
    difference_quantity INTEGER,
    note TEXT,
    CONSTRAINT fk_inv_stocktake_item_session
        FOREIGN KEY (session_id) REFERENCES inv_stocktake_session(id) ON DELETE CASCADE,
    CONSTRAINT uk_inv_stocktake_item_session_book UNIQUE (session_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_inv_stocktake_session_status
    ON inv_stocktake_session(status);

CREATE INDEX IF NOT EXISTS idx_inv_stocktake_session_assigned_staff
    ON inv_stocktake_session(assigned_staff_id);

CREATE INDEX IF NOT EXISTS idx_inv_stocktake_item_book
    ON inv_stocktake_item(book_id);
