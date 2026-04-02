-- V8: Khởi tạo module Order (Quản lý Đơn hàng)
-- Thiết lập cấu trúc hỗ trợ Saga State Machine và Snapshot dữ liệu

CREATE TABLE ord_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    
    -- Business Status
    status VARCHAR(30) NOT NULL, -- PENDING_PAYMENT, PAID, CANCELLED, COMPLETED
    
    -- Saga State Machine
    saga_status VARCHAR(30) NOT NULL, -- INIT, STOCK_RESERVED, COUPON_RESERVED, COMPLETED, COMPENSATED, FAILED
    
    -- Snapshot info
    total_amount DECIMAL(19, 2) NOT NULL,
    discount_amount DECIMAL(19, 2) DEFAULT 0,
    shipping_address TEXT NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    
    expired_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Idempotency by RequestId
CREATE UNIQUE INDEX idx_ord_order_request_id ON ord_order(request_id);

-- Performance Indexes
CREATE INDEX idx_ord_order_status ON ord_order(status);
CREATE INDEX idx_ord_order_saga_status ON ord_order(saga_status);
CREATE INDEX idx_ord_order_expired_at ON ord_order(expired_at);
CREATE INDEX idx_ord_order_user_id ON ord_order(user_id);

CREATE TABLE ord_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES ord_order(id),
    book_id BIGINT NOT NULL,
    
    -- Item Snapshot
    book_title VARCHAR(255) NOT NULL,
    price_at_purchase DECIMAL(19, 2) NOT NULL,
    quantity INT NOT NULL,
    
    CONSTRAINT fk_ord_item_order FOREIGN KEY (order_id) REFERENCES ord_order(id)
);

CREATE INDEX idx_ord_item_order_id ON ord_order_item(order_id);
