-- V13__init_returns_module.sql
-- Create returns module tables with 'ret_' prefix

CREATE TABLE ret_return_requests (
    id VARCHAR(36) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    refund_amount DECIMAL(19, 2),
    reason VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ret_return_items (
    id VARCHAR(36) PRIMARY KEY,
    return_request_id VARCHAR(36) NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    refund_price DECIMAL(19, 2) NOT NULL,
    item_condition VARCHAR(20) NOT NULL,
    CONSTRAINT fk_ret_items_request FOREIGN KEY (return_request_id) REFERENCES ret_return_requests(id)
);

CREATE TABLE ret_return_histories (
    id VARCHAR(36) PRIMARY KEY,
    return_request_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    CONSTRAINT fk_ret_histories_request FOREIGN KEY (return_request_id) REFERENCES ret_return_requests(id)
);

CREATE TABLE ret_outbox_event (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
