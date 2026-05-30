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
    'Effective Java',
    'Joshua Bloch',
    'Effective Java la sach lap trinh Java chuyen sau ve thiet ke API, generics, enum, lambda, concurrency va cac best practices giup lap trinh vien Java viet code ro rang, an toan va de bao tri.',
    420000,
    12,
    'Addison-Wesley',
    '9780134685991',
    2018,
    'Tieng Anh',
    '["lap trinh java","java","backend","api design","generics","concurrency","best practices"]'::jsonb,
    416,
    'Paperback',
    480000,
    4.85,
    90,
    NULL,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM cat_book WHERE isbn = '9780134685991');

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Lap trinh'
WHERE b.isbn = '9780134685991'
ON CONFLICT DO NOTHING;

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Lập trình'
WHERE b.isbn = '9780134685991'
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, deprecated_quantity, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = '9780134685991'
ON CONFLICT (book_id) DO UPDATE
SET quantity = EXCLUDED.quantity,
    updated_at = NOW();
