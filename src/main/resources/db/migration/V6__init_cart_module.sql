-- Table: crt_cart
CREATE TABLE crt_cart (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: crt_cart_item
CREATE TABLE crt_cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_add_time DECIMAL(19, 2) NOT NULL,
    title_snapshot VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES crt_cart(id) ON DELETE CASCADE,
    UNIQUE(cart_id, book_id)
);

CREATE INDEX idx_crt_cart_user_id ON crt_cart(user_id);
CREATE INDEX idx_crt_cart_item_cart_id ON crt_cart_item(cart_id);
