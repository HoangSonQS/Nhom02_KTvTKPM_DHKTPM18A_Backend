-- Migration: V10__init_notification_and_admin_report.sql
-- Description: Khởi tạo bảng Log thông báo và bảng Báo cáo Admin (CQRS Read Model)

-- 1. Bảng Log Thông báo (Idempotency & Retry tracking)
CREATE TABLE ntf_notification_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT,
    status VARCHAR(20) NOT NULL, -- INIT, SUCCESS, FAILED, FAILED_PERMANENT
    channel VARCHAR(10), -- EMAIL, SMS, PUSH
    attempt_count INT DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_ntf_status ON ntf_notification_log(status);

-- 2. Bảng Báo cáo Đơn hàng (Admin Analytics Read Model)
CREATE TABLE adm_order_report (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    customer_name VARCHAR(255),
    total_amount DECIMAL(19, 2),
    status VARCHAR(20), -- PENDING_PAYMENT, PAID, CANCELLED
    coupon_code VARCHAR(50),
    items_summary TEXT, -- Danh sách sản phẩm rút gọn
    payment_method VARCHAR(50),
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checkout_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_adm_status ON adm_order_report(status);
CREATE INDEX idx_adm_created_at ON adm_order_report(created_at);
CREATE INDEX idx_order_report_paid_checkout ON adm_order_report (paid_at, checkout_at) WHERE paid_at IS NOT NULL;
