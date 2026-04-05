-- Migration: V12__init_logistics_module.sql
-- Description: Khởi tạo các bảng cho module Logistics và Outbox Pattern

-- 1. Bảng Nhà cung cấp (Supplier)
CREATE TABLE log_supplier (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(100),
    phone_number VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    tax_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Đơn mua hàng (Purchase Order)
CREATE TABLE log_purchase_order (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES log_supplier(id),
    status VARCHAR(20) NOT NULL, -- DRAFT, SUBMITTED, APPROVED, RECEIVED, CANCELLED
    total_amount DECIMAL(19, 2) DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    received_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    cancel_reason TEXT,
    cancelled_by VARCHAR(100),
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    note TEXT
);

CREATE INDEX idx_log_po_status ON log_purchase_order(status);
CREATE INDEX idx_log_po_supplier ON log_purchase_order(supplier_id);

-- 3. Bảng Chi tiết Đơn mua hàng (Purchase Order Item)
CREATE TABLE log_purchase_order_item (
    id BIGSERIAL PRIMARY KEY,
    po_id BIGINT NOT NULL REFERENCES log_purchase_order(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_at_order DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Lịch sử trạng thái PO (Purchase Order History)
CREATE TABLE log_purchase_order_history (
    id BIGSERIAL PRIMARY KEY,
    po_id BIGINT NOT NULL REFERENCES log_purchase_order(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT
);

-- 5. Bảng Transactional Outbox (Guaranteed Event Delivery)
CREATE TABLE log_outbox_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL, -- JSON format
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_log_outbox_status ON log_outbox_event(status);

-- 6. Bảng Idempotency cho Inventory (Processed Events)
CREATE TABLE inv_processed_event (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
