-- V18__add_book_details.sql
-- Bổ sung siêu dữ liệu cho sách & Tái kiến trúc AI Vector Store

-- 1. KÍCH HOẠT EXTENSION UNACCENT CHO TIẾNG VIỆT
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2. ĐỊNH HÌNH LẠI TABLE CAT_BOOKS (Safe Schema Evolution)
ALTER TABLE cat_book RENAME COLUMN quantity TO deprecated_quantity;

-- Thêm metadata xuất bản & thực tế
ALTER TABLE cat_book ADD COLUMN publisher VARCHAR(255);
ALTER TABLE cat_book ADD COLUMN isbn VARCHAR(20);
ALTER TABLE cat_book ADD COLUMN publication_year INT;
ALTER TABLE cat_book ADD COLUMN language VARCHAR(50);
ALTER TABLE cat_book ADD COLUMN keywords JSONB;

ALTER TABLE cat_book ADD COLUMN page_count INT;
ALTER TABLE cat_book ADD COLUMN cover_type VARCHAR(50);
ALTER TABLE cat_book ADD COLUMN weight INT; 
ALTER TABLE cat_book ADD COLUMN length INT;
ALTER TABLE cat_book ADD COLUMN width INT;
ALTER TABLE cat_book ADD COLUMN height INT;

ALTER TABLE cat_book ADD COLUMN original_price NUMERIC(19, 2);
ALTER TABLE cat_book ADD COLUMN average_rating NUMERIC(3,2) DEFAULT 0.00;
ALTER TABLE cat_book ADD COLUMN rating_count INT DEFAULT 0;

-- Ràng buộc Unique cho ISBN
ALTER TABLE cat_book ADD CONSTRAINT uk_books_isbn UNIQUE (isbn);

-- 3. TÍCH HỢP FULL-TEXT SEARCH RANKING
ALTER TABLE cat_book ADD COLUMN tsv tsvector;
CREATE INDEX idx_books_tsv ON cat_book USING GIN(tsv);
CREATE INDEX idx_books_keywords_jsonb ON cat_book USING GIN (keywords);

-- Tạo Trigger Parse JSONB và gọt Dấu Tiếng Việt (chống nhiễu chuỗi JSON)
CREATE OR REPLACE FUNCTION catalog_books_tsvector_trigger() RETURNS trigger AS $$
DECLARE
    clean_keywords TEXT;
BEGIN
  -- Trích xuất value từ mảng JSONB array thành Text sạch "keyword1 keyword2"
  IF NEW.keywords IS NOT NULL AND jsonb_array_length(NEW.keywords) > 0 THEN
      SELECT string_agg(val, ' ') INTO clean_keywords 
      FROM jsonb_array_elements_text(NEW.keywords) AS val;
  ELSE
      clean_keywords := '';
  END IF;

  -- Build TSVECTOR tích hợp Unaccent để trị tiếng Việt
  NEW.tsv := setweight(to_tsvector('simple', unaccent(COALESCE(NEW.title, ''))), 'A') || 
             setweight(to_tsvector('simple', unaccent(COALESCE(clean_keywords, ''))), 'B');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trq_books_tsv_update
BEFORE INSERT OR UPDATE ON cat_book
FOR EACH ROW EXECUTE FUNCTION catalog_books_tsvector_trigger();

-- 4. TÁCH BẢNG CÁC TRƯỜNG NỘI DUNG LỚN (Content Segregation)
CREATE TABLE cat_book_contents (
    book_id BIGINT PRIMARY KEY REFERENCES cat_book(id) ON DELETE CASCADE,
    table_of_contents TEXT,
    excerpt TEXT
);

-- 5. NÂNG CẤP CƠ CHẾ ĐẢM BẢO TÍNH NHẤT QUÁN CHO AI MODULE
-- Bảng lưu vết các event đã xử lý kèm trạng thái để đảm bảo Idempotency & Retry-safe
CREATE TABLE ai_processed_events (
    event_id UUID PRIMARY KEY,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bổ sung Versioning & Integrity Check cho Vector Store
ALTER TABLE ai_book_vectors ADD COLUMN embedding_version INT DEFAULT 1;
ALTER TABLE ai_book_vectors ADD COLUMN content_hash VARCHAR(64); -- SHA-256
