CREATE TABLE IF NOT EXISTS prm_flash_sale (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    sale_quantity INTEGER NOT NULL,
    discount_percent INTEGER NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_prm_flash_sale_quantity CHECK (sale_quantity > 0),
    CONSTRAINT chk_prm_flash_sale_discount CHECK (discount_percent BETWEEN 1 AND 90),
    CONSTRAINT chk_prm_flash_sale_time CHECK (end_at > start_at)
);

CREATE INDEX IF NOT EXISTS idx_prm_flash_sale_active_time
    ON prm_flash_sale (is_active, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_prm_flash_sale_book
    ON prm_flash_sale (book_id);
