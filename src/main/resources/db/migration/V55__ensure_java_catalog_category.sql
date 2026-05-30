INSERT INTO cat_category (name, is_active)
VALUES ('Lap trinh', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO cat_book_category (book_id, category_id)
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON c.name = 'Lap trinh'
WHERE b.isbn = '9780134685991'
ON CONFLICT DO NOTHING;
