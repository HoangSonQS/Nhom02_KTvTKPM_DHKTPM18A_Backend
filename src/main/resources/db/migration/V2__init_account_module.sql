-- Flyway Migration V2: Khởi tạo schema cho module account
-- Prefix bảng: acc_ (module account)

-- Bảng Account Profile (Lưu thông tin cá nhân mở rộng)
CREATE TABLE acc_account
(
    id               BIGSERIAL    NOT NULL,
    user_id          BIGINT       NOT NULL UNIQUE, -- Tham chiếu mềm tới auth_user.id
    phone_number     VARCHAR(20),
    avatar_url       VARCHAR(500),
    avatar_public_id VARCHAR(255),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    CONSTRAINT pk_acc_account PRIMARY KEY (id)
);

-- Index cho user_id vì tần suất query theo user_id (từ JWT) rất cao
CREATE INDEX idx_acc_user_id ON acc_account (user_id);

-- Bảng Address (Value Object/Entity thuộc Account Aggregate)
CREATE TABLE acc_address
(
    id         BIGSERIAL    NOT NULL,
    account_id BIGINT       NOT NULL,
    street     VARCHAR(200),
    ward       VARCHAR(100),
    district   VARCHAR(100),
    city       VARCHAR(100),
    is_default BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT pk_acc_address PRIMARY KEY (id),
    CONSTRAINT fk_acc_address_account FOREIGN KEY (account_id) REFERENCES acc_account (id) ON DELETE CASCADE
);

CREATE INDEX idx_acc_address_account_id ON acc_address (account_id);
