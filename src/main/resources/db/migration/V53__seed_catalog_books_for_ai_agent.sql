INSERT INTO cat_category (name, is_active)
VALUES
    ('Lập trình', TRUE),
    ('Phát triển bản thân', TRUE),
    ('Văn học hiện đại', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO cat_book (
    title,
    author,
    description,
    price,
    deprecated_quantity,
    publisher,
    isbn,
    publication_year,
    language,
    keywords,
    page_count,
    cover_type,
    original_price,
    average_rating,
    rating_count,
    image_url,
    is_active,
    created_at,
    updated_at
)
SELECT
    'Clean Code',
    'Robert C. Martin',
    'Clean Code trình bày các nguyên tắc viết mã nguồn dễ đọc, dễ bảo trì và dễ mở rộng. Sách phù hợp với lập trình viên muốn nâng chất lượng thiết kế, đặt tên, kiểm thử và refactor trong dự án thực tế.',
    350000,
    15,
    'Prentice Hall',
    '9780132350884',
    2008,
    'Tiếng Anh',
    '["lap trinh","clean code","software engineering","refactoring","code quality"]'::jsonb,
    464,
    'Paperback',
    420000,
    4.80,
    128,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9780132350884');

INSERT INTO cat_book (
    title, author, description, price, deprecated_quantity, publisher, isbn,
    publication_year, language, keywords, page_count, cover_type, original_price,
    average_rating, rating_count, image_url, is_active, created_at, updated_at
)
SELECT
    'Atomic Habits',
    'James Clear',
    'Atomic Habits giải thích cách hình thành thói quen nhỏ nhưng tạo ra thay đổi lớn theo thời gian. Nội dung tập trung vào hệ thống hành vi, môi trường, tín hiệu và phần thưởng để xây dựng lối sống kỷ luật hơn.',
    189000,
    20,
    'Avery',
    '9780735211292',
    2018,
    'Tiếng Anh',
    '["ky nang song","thoi quen","phat trien ban than","nang suat","hanh vi"]'::jsonb,
    320,
    'Paperback',
    230000,
    4.90,
    215,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9780735211292');

INSERT INTO cat_book (
    title, author, description, price, deprecated_quantity, publisher, isbn,
    publication_year, language, keywords, page_count, cover_type, original_price,
    average_rating, rating_count, image_url, is_active, created_at, updated_at
)
SELECT
    'Đắc Nhân Tâm',
    'Dale Carnegie',
    'Đắc Nhân Tâm là tác phẩm kinh điển về giao tiếp, ứng xử và xây dựng quan hệ. Sách đưa ra những nguyên tắc thực tế để thấu hiểu người khác, tạo thiện cảm và giải quyết mâu thuẫn trong công việc lẫn cuộc sống.',
    99000,
    30,
    'Simon & Schuster',
    '9780671027032',
    1936,
    'Tiếng Việt',
    '["ky nang song","giao tiep","ung xu","phat trien ban than","quan he"]'::jsonb,
    320,
    'Paperback',
    130000,
    4.70,
    180,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9780671027032');

INSERT INTO cat_book (
    title, author, description, price, deprecated_quantity, publisher, isbn,
    publication_year, language, keywords, page_count, cover_type, original_price,
    average_rating, rating_count, image_url, is_active, created_at, updated_at
)
SELECT
    'Nhà Giả Kim',
    'Paulo Coelho',
    'Nhà Giả Kim kể về hành trình theo đuổi ước mơ của chàng chăn cừu Santiago. Tác phẩm giàu tính biểu tượng, khơi gợi niềm tin vào trực giác, lựa chọn cá nhân và ý nghĩa của hành trình trưởng thành.',
    89000,
    25,
    'HarperOne',
    '9780061122415',
    1988,
    'Tiếng Việt',
    '["van hoc","truyen cam hung","uoc mo","hanh trinh","truong thanh"]'::jsonb,
    208,
    'Paperback',
    120000,
    4.60,
    150,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9780061122415');

INSERT INTO cat_book (
    title, author, description, price, deprecated_quantity, publisher, isbn,
    publication_year, language, keywords, page_count, cover_type, original_price,
    average_rating, rating_count, image_url, is_active, created_at, updated_at
)
SELECT
    'Tôi Thấy Hoa Vàng Trên Cỏ Xanh',
    'Nguyễn Nhật Ánh',
    'Tôi Thấy Hoa Vàng Trên Cỏ Xanh là câu chuyện trong trẻo về tuổi thơ, tình anh em và những rung động đầu đời ở miền quê Việt Nam. Giọng văn nhẹ nhàng, giàu cảm xúc và phù hợp với độc giả yêu văn học hiện đại.',
    125000,
    18,
    'NXB Trẻ',
    '9786041134537',
    2010,
    'Tiếng Việt',
    '["nguyen nhat anh","van hoc hien dai","tuoi tho","thieu nhi","viet nam"]'::jsonb,
    378,
    'Paperback',
    150000,
    4.75,
    96,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9786041134537');

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Lập trình'
WHERE b.isbn = '9780132350884'
ON CONFLICT DO NOTHING;

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Phát triển bản thân'
WHERE b.isbn IN ('9780735211292', '9780671027032')
ON CONFLICT DO NOTHING;

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Văn học hiện đại'
WHERE b.isbn IN ('9780061122415', '9786041134537')
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, deprecated_quantity, 0, NOW(), NOW()
FROM cat_book
WHERE isbn IN ('9780132350884', '9780735211292', '9780671027032', '9780061122415', '9786041134537')
ON CONFLICT (book_id) DO UPDATE
SET quantity = EXCLUDED.quantity,
    updated_at = NOW();
