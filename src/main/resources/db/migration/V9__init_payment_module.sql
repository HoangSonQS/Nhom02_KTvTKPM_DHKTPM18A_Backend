-- V9__init_payment_module.sql
CREATE TABLE pay_payment (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    transaction_id VARCHAR(100),
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'VNPAY',
    status VARCHAR(20) NOT NULL, -- PENDING, SUCCESS, FAILED
    result_data TEXT, -- FULL JSON RESPONSE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pay_payment_order_id ON pay_payment(order_id);
CREATE INDEX idx_pay_payment_transaction_id ON pay_payment(transaction_id);
