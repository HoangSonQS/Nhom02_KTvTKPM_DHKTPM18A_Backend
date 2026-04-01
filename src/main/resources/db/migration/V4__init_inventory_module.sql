-- Table: inv_stock
CREATE TABLE inv_stock (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL UNIQUE,
    quantity INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: inv_stock_history
CREATE TABLE inv_stock_history (
    id BIGSERIAL PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL UNIQUE,
    book_id BIGINT NOT NULL,
    amount INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    request_data JSONB,
    result_data JSONB
);

CREATE INDEX idx_inv_stock_book_id ON inv_stock(book_id);
CREATE INDEX idx_inv_history_ref_id ON inv_stock_history(reference_id);
CREATE INDEX idx_inv_history_book_id ON inv_stock_history(book_id);
