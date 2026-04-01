-- Table: prm_coupon
CREATE TABLE prm_coupon (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(19, 2) NOT NULL,
    min_order_value DECIMAL(19, 2),
    max_discount_value DECIMAL(19, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prm_coupon_code ON prm_coupon(code);
