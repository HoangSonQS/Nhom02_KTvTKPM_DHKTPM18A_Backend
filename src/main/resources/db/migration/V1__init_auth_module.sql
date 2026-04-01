-- Flyway Migration V1: Khởi tạo schema cho module auth
-- Prefix bảng: auth_ (module auth)
-- Tuân thủ rule: Không có cross-module FK

-- Bảng người dùng chính (auth credentials + role)
CREATE TABLE auth_user
(
    id         BIGSERIAL    NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT pk_auth_user PRIMARY KEY (id),
    CONSTRAINT chk_auth_user_role CHECK (role IN ('ADMIN', 'STAFF', 'CUSTOMER'))
);

-- Index tìm kiếm theo email (dùng cho login)
CREATE INDEX idx_auth_user_email ON auth_user (email);

-- Seed dữ liệu: Admin mặc định
-- Password: Admin@1234 (BCrypt hash — thay đổi trước khi deploy production!)
INSERT INTO auth_user (email, password, full_name, role, enabled)
VALUES ('admin@sebook.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCjv5MRJhSAMVEXvJaXCGJO',
        'System Administrator',
        'ADMIN',
        TRUE);
