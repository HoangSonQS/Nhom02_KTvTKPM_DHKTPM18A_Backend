-- Flyway Migration V3: Khởi tạo schema cho module catalog
-- Prefix bảng: cat_ (module catalog)

-- Bảng Category (Aggregate Root riêng)
CREATE TABLE cat_category
(
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(100) NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT pk_cat_category PRIMARY KEY (id)
);

-- Bảng Book (Aggregate Root)
CREATE TABLE cat_book
(
    id               BIGSERIAL    NOT NULL,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(100) NOT NULL,
    description      TEXT,
    price            DECIMAL(19, 2) NOT NULL DEFAULT 0,
    quantity         INT          NOT NULL DEFAULT 0,
    image_url        VARCHAR(500),
    image_public_id  VARCHAR(255),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    CONSTRAINT pk_cat_book PRIMARY KEY (id)
);

-- Bảng liên kết Many-to-Many giữa Book và Category
-- Chú ý: Dùng ElementCollection mapping bên JPA nên tên bảng là cat_book_category
CREATE TABLE cat_book_category
(
    book_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_cat_book_category PRIMARY KEY (book_id, category_id),
    CONSTRAINT fk_cat_book_category_book FOREIGN KEY (book_id) REFERENCES cat_book (id) ON DELETE CASCADE,
    CONSTRAINT fk_cat_book_category_category FOREIGN KEY (category_id) REFERENCES cat_category (id) ON DELETE CASCADE
);

-- Indexes cho việc tìm kiếm
CREATE INDEX idx_cat_book_title ON cat_book (title);
CREATE INDEX idx_cat_book_author ON cat_book (author);

-- Seed dữ liệu mẫu cho Category
INSERT INTO cat_category (name) VALUES ('Văn học'), ('Kinh tế'), ('Kỹ năng sống'), ('Thiếu nhi'), ('Công nghệ thông tin');
