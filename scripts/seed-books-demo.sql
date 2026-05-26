-- Demo book database for SEBook.
-- Run after Flyway migrations. Safe to re-run: books are upserted by ISBN.

BEGIN;

INSERT INTO cat_category (name, is_active, created_at, updated_at)
VALUES
    ('Văn học', TRUE, NOW(), NOW()),
    ('Kinh tế', TRUE, NOW(), NOW()),
    ('Kỹ năng sống', TRUE, NOW(), NOW()),
    ('Thiếu nhi', TRUE, NOW(), NOW()),
    ('Công nghệ thông tin', TRUE, NOW(), NOW()),
    ('Ngoại văn', TRUE, NOW(), NOW()),
    ('Manga', TRUE, NOW(), NOW()),
    ('Giáo trình', TRUE, NOW(), NOW()),
    ('Lịch sử', TRUE, NOW(), NOW()),
    ('Tâm lý', TRUE, NOW(), NOW())
ON CONFLICT (name) DO UPDATE
SET is_active = TRUE,
    updated_at = NOW();

DROP TABLE IF EXISTS tmp_seed_books;

CREATE TEMP TABLE tmp_seed_books (
    title VARCHAR(255),
    author VARCHAR(100),
    category_name VARCHAR(100),
    description TEXT,
    price NUMERIC(19, 2),
    deprecated_quantity INT,
    image_url VARCHAR(500),
    publisher VARCHAR(255),
    isbn VARCHAR(20),
    publication_year INT,
    language VARCHAR(50),
    keywords JSONB,
    page_count INT,
    cover_type VARCHAR(50),
    weight INT,
    length INT,
    width INT,
    height INT,
    original_price NUMERIC(19, 2),
    average_rating NUMERIC(3, 2),
    rating_count INT,
    created_at TIMESTAMP
) ON COMMIT DROP;

INSERT INTO tmp_seed_books (
    title, author, category_name, description, price, deprecated_quantity, image_url,
    publisher, isbn, publication_year, language, keywords, page_count, cover_type,
    weight, length, width, height, original_price, average_rating, rating_count, created_at
)
VALUES
    (
        'Nhà Giả Kim', 'Paulo Coelho', 'Văn học',
        'Câu chuyện biểu tượng về hành trình theo đuổi ước mơ và lắng nghe trái tim.',
        89000, 64, 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=500&q=80',
        'Nhã Nam', 'SEBOOK-DEMO-001', 2022, 'Vietnamese',
        '["văn học","triết lý","ước mơ"]'::jsonb, 228, 'Bìa mềm',
        280, 200, 130, 18, 120000, 4.70, 31, NOW() - INTERVAL '1 day'
    ),
    (
        'Đắc Nhân Tâm', 'Dale Carnegie', 'Kỹ năng sống',
        'Những nguyên tắc giao tiếp kinh điển giúp xây dựng quan hệ tốt hơn trong công việc và cuộc sống.',
        98000, 72, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=500&q=80',
        'Tổng hợp TP.HCM', 'SEBOOK-DEMO-002', 2023, 'Vietnamese',
        '["giao tiếp","kỹ năng sống","thành công"]'::jsonb, 320, 'Bìa mềm',
        360, 205, 145, 22, 135000, 4.80, 52, NOW() - INTERVAL '2 day'
    ),
    (
        'Tuổi Trẻ Đáng Giá Bao Nhiêu', 'Rosie Nguyễn', 'Kỹ năng sống',
        'Những ghi chép truyền cảm hứng về học tập, trải nghiệm và phát triển bản thân.',
        92000, 48, 'https://images.unsplash.com/photo-1519682337058-a94d519337bc?auto=format&fit=crop&w=500&q=80',
        'Hội Nhà Văn', 'SEBOOK-DEMO-003', 2021, 'Vietnamese',
        '["tuổi trẻ","phát triển bản thân","trải nghiệm"]'::jsonb, 285, 'Bìa mềm',
        340, 205, 145, 20, 125000, 4.30, 19, NOW() - INTERVAL '3 day'
    ),
    (
        'Tôi Thấy Hoa Vàng Trên Cỏ Xanh', 'Nguyễn Nhật Ánh', 'Văn học',
        'Một lát cắt tuổi thơ trong trẻo, ấm áp và nhiều dư vị của văn học Việt Nam hiện đại.',
        110000, 55, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=500&q=80',
        'Trẻ', 'SEBOOK-DEMO-004', 2022, 'Vietnamese',
        '["văn học Việt Nam","tuổi thơ","Nguyễn Nhật Ánh"]'::jsonb, 380, 'Bìa mềm',
        430, 205, 145, 28, 150000, 4.60, 28, NOW() - INTERVAL '4 day'
    ),
    (
        'Dế Mèn Phiêu Lưu Ký', 'Tô Hoài', 'Thiếu nhi',
        'Tác phẩm thiếu nhi kinh điển về hành trình trưởng thành, tình bạn và lòng dũng cảm.',
        75000, 80, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=500&q=80',
        'Kim Đồng', 'SEBOOK-DEMO-005', 2021, 'Vietnamese',
        '["thiếu nhi","văn học Việt Nam","phiêu lưu"]'::jsonb, 180, 'Bìa mềm',
        260, 200, 130, 18, 99000, 4.70, 25, NOW() - INTERVAL '5 day'
    ),
    (
        'Harry Potter and the Sorcerer''s Stone', 'J.K. Rowling', 'Ngoại văn',
        'The first magical adventure of Harry Potter, full of friendship, courage and wonder.',
        150000, 24, 'https://books.google.com/books/content?id=wrOQLV6xB-wC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Bloomsbury', 'SEBOOK-DEMO-006', 2018, 'English',
        '["fantasy","children","harry potter"]'::jsonb, 320, 'Paperback',
        420, 205, 135, 25, 210000, 4.90, 57, NOW() - INTERVAL '6 day'
    ),
    (
        'Clean Code - Bản thực hành', 'Robert C. Martin', 'Công nghệ thông tin',
        'Cuốn sách kinh điển về cách viết mã sạch, dễ đọc và dễ bảo trì cho lập trình viên.',
        189000, 42, 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=500&q=80',
        'SEBook Demo', 'SEBOOK-DEMO-007', 2024, 'Vietnamese',
        '["lập trình","clean code","software"]'::jsonb, 420, 'Bìa mềm',
        550, 240, 160, 30, 230000, 4.70, 18, NOW() - INTERVAL '1 day'
    ),
    (
        'The Pragmatic Programmer', 'Andrew Hunt, David Thomas', 'Công nghệ thông tin',
        'Sổ tay tư duy nghề nghiệp dành cho lập trình viên muốn xây dựng phần mềm tốt hơn.',
        601000, 28, 'https://books.google.com/books/content?id=5wBQEp6ruIAC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Addison-Wesley', 'SEBOOK-DEMO-008', 2020, 'English',
        '["programming","craft","engineering"]'::jsonb, 352, 'Paperback',
        620, 235, 178, 28, 858000, 4.50, 16, NOW() - INTERVAL '2 day'
    ),
    (
        'Kinh Doanh Tinh Gọn', 'Eric Ries', 'Kinh tế',
        'Phương pháp thử nghiệm nhanh, học hỏi nhanh và xây dựng sản phẩm phù hợp thị trường.',
        159000, 31, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=500&q=80',
        'Lao Động', 'SEBOOK-DEMO-009', 2023, 'Vietnamese',
        '["startup","kinh doanh","lean"]'::jsonb, 320, 'Bìa mềm',
        430, 210, 140, 24, 199000, 4.60, 21, NOW() - INTERVAL '9 day'
    ),
    (
        'Tư Duy Nhanh Và Chậm', 'Daniel Kahneman', 'Tâm lý',
        'Một hành trình khám phá hai hệ thống tư duy tác động đến quyết định hằng ngày.',
        179000, 45, 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=500&q=80',
        'Thế Giới', 'SEBOOK-DEMO-010', 2022, 'Vietnamese',
        '["tâm lý","tư duy","quyết định"]'::jsonb, 520, 'Bìa mềm',
        680, 240, 160, 35, 239000, 4.80, 34, NOW() - INTERVAL '10 day'
    ),
    (
        'Sapiens - Lược Sử Loài Người', 'Yuval Noah Harari', 'Lịch sử',
        'Một góc nhìn rộng về lịch sử, văn minh và quá trình con người định hình thế giới.',
        199000, 33, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=500&q=80',
        'Thế Giới', 'SEBOOK-DEMO-011', 2021, 'Vietnamese',
        '["lịch sử","nhân loại","xã hội"]'::jsonb, 512, 'Bìa mềm',
        720, 240, 160, 35, 259000, 4.60, 24, NOW() - INTERVAL '12 day'
    ),
    (
        'Atomic Habits', 'James Clear', 'Kỹ năng sống',
        'Cách xây dựng thói quen nhỏ tạo nên thay đổi lớn và bền vững.',
        165000, 50, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=500&q=80',
        'Penguin Random House', 'SEBOOK-DEMO-012', 2024, 'Vietnamese',
        '["thói quen","kỹ năng sống","năng suất"]'::jsonb, 336, 'Bìa mềm',
        420, 210, 145, 25, 220000, 4.90, 42, NOW() - INTERVAL '13 day'
    ),
    (
        'One Piece Tập 1', 'Eiichiro Oda', 'Manga',
        'Khởi đầu chuyến hải trình của Luffy trên con đường trở thành Vua Hải Tặc.',
        25000, 110, 'https://images.unsplash.com/photo-1608889476561-6242cfdbf622?auto=format&fit=crop&w=500&q=80',
        'Kim Đồng', 'SEBOOK-DEMO-013', 2020, 'Vietnamese',
        '["manga","phiêu lưu","one piece"]'::jsonb, 200, 'Bìa mềm',
        185, 176, 113, 12, 30000, 4.80, 66, NOW() - INTERVAL '15 day'
    ),
    (
        'Doraemon Tập 1', 'Fujiko F. Fujio', 'Manga',
        'Những câu chuyện vui nhộn và giàu trí tưởng tượng về chú mèo máy Doraemon.',
        25000, 100, 'https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?auto=format&fit=crop&w=500&q=80',
        'Kim Đồng', 'SEBOOK-DEMO-014', 2020, 'Vietnamese',
        '["manga","thiếu nhi","doraemon"]'::jsonb, 188, 'Bìa mềm',
        180, 176, 113, 12, 30000, 4.70, 61, NOW() - INTERVAL '16 day'
    ),
    (
        'Conan Tập 1', 'Gosho Aoyama', 'Manga',
        'Vụ án đầu tiên của thám tử lừng danh Conan trong hình hài học sinh tiểu học.',
        25000, 120, 'https://images.unsplash.com/photo-1618519764620-7403abdbdfe9?auto=format&fit=crop&w=500&q=80',
        'Kim Đồng', 'SEBOOK-DEMO-015', 2020, 'Vietnamese',
        '["manga","trinh thám","conan"]'::jsonb, 188, 'Bìa mềm',
        180, 176, 113, 12, 30000, 4.60, 40, NOW() - INTERVAL '17 day'
    ),
    (
        'Giáo Trình Tiếng Anh Giao Tiếp', 'SEBook Academy', 'Giáo trình',
        'Bộ bài học giao tiếp tiếng Anh theo tình huống cho người mới bắt đầu.',
        120000, 70, 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=500&q=80',
        'SEBook Demo', 'SEBOOK-DEMO-016', 2024, 'Vietnamese',
        '["giáo trình","tiếng Anh","giao tiếp"]'::jsonb, 260, 'Bìa mềm',
        410, 240, 170, 20, 150000, 4.20, 8, NOW() - INTERVAL '18 day'
    ),
    (
        'Lược Sử Thời Gian', 'Stephen Hawking', 'Công nghệ thông tin',
        'Một lối dẫn nhập dễ tiếp cận vào vũ trụ học, thời gian và các câu hỏi lớn của khoa học.',
        145000, 27, 'https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=500&q=80',
        'Trẻ', 'SEBOOK-DEMO-017', 2022, 'Vietnamese',
        '["khoa học","vũ trụ","thời gian"]'::jsonb, 256, 'Bìa mềm',
        350, 205, 145, 22, 190000, 4.50, 17, NOW() - INTERVAL '19 day'
    ),
    (
        'Bố Già', 'Mario Puzo', 'Văn học',
        'Tiểu thuyết kinh điển về quyền lực, gia đình và thế giới ngầm nước Mỹ.',
        160000, 36, 'https://images.unsplash.com/photo-1507842217343-583bb7270b66?auto=format&fit=crop&w=500&q=80',
        'Đông A', 'SEBOOK-DEMO-018', 2021, 'Vietnamese',
        '["tiểu thuyết","kinh điển","gia đình"]'::jsonb, 520, 'Bìa mềm',
        650, 240, 160, 34, 210000, 4.60, 26, NOW() - INTERVAL '20 day'
    ),
    (
        'Không Gia Đình', 'Hector Malot', 'Văn học',
        'Câu chuyện cảm động về hành trình trưởng thành, lòng nhân ái và khát vọng thuộc về.',
        135000, 41, 'https://images.unsplash.com/photo-1526243741027-444d633d7365?auto=format&fit=crop&w=500&q=80',
        'Văn học', 'SEBOOK-DEMO-019', 2020, 'Vietnamese',
        '["văn học","kinh điển","thiếu nhi"]'::jsonb, 480, 'Bìa mềm',
        590, 210, 145, 32, 180000, 4.40, 22, NOW() - INTERVAL '21 day'
    ),
    (
        'Hoàng Tử Bé', 'Antoine de Saint-Exupéry', 'Thiếu nhi',
        'Một câu chuyện nhỏ giàu chất thơ về tình bạn, sự cô đơn và cách nhìn thế giới bằng trái tim.',
        69000, 75, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=500&q=80',
        'Hội Nhà Văn', 'SEBOOK-DEMO-020', 2022, 'Vietnamese',
        '["thiếu nhi","triết lý","kinh điển"]'::jsonb, 112, 'Bìa cứng',
        220, 190, 130, 15, 99000, 4.80, 44, NOW() - INTERVAL '22 day'
    ),
    (
        'Principles', 'Ray Dalio', 'Kinh tế',
        'Những nguyên tắc làm việc và ra quyết định được đúc kết từ kinh nghiệm đầu tư và quản trị.',
        260000, 18, 'https://images.unsplash.com/photo-1554224155-6726b3ff858f?auto=format&fit=crop&w=500&q=80',
        'Simon & Schuster', 'SEBOOK-DEMO-021', 2017, 'English',
        '["business","finance","principles"]'::jsonb, 592, 'Hardcover',
        850, 240, 165, 38, 350000, 4.40, 13, NOW() - INTERVAL '23 day'
    ),
    (
        'The Picture of Dorian Gray', 'Oscar Wilde', 'Ngoại văn',
        'A gothic philosophical novel about beauty, desire and moral decay.',
        85000, 22, 'https://books.google.com/books/content?id=QZ1GAAAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Penguin Classics', 'SEBOOK-DEMO-022', 2012, 'English',
        '["classic","gothic","literature"]'::jsonb, 304, 'Paperback',
        300, 198, 129, 22, 120000, 4.50, 20, NOW() - INTERVAL '24 day'
    ),
    (
        'TinyML', 'Pete Warden', 'Công nghệ thông tin',
        'Giới thiệu machine learning trên thiết bị nhỏ, phù hợp cho IoT và hệ thống nhúng.',
        227000, 36, 'https://books.google.com/books/content?id=Y4bUDwAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'O''Reilly Media', 'SEBOOK-DEMO-023', 2020, 'English',
        '["ai","machine learning","iot"]'::jsonb, 504, 'Bìa mềm',
        780, 235, 178, 32, 319000, 4.20, 9, NOW() - INTERVAL '25 day'
    ),
    (
        'Self and Family', 'Jane Cary Peck', 'Tâm lý',
        'Một cuốn sách nhập môn về bản thân, gia đình và các mối quan hệ trong đời sống thường ngày.',
        56000, 44, 'https://images.unsplash.com/photo-1511108690759-009324a90311?auto=format&fit=crop&w=500&q=80',
        'SEBook Demo', 'SEBOOK-DEMO-024', 2019, 'English',
        '["family","psychology","self"]'::jsonb, 210, 'Paperback',
        260, 198, 129, 18, 79000, 4.10, 7, NOW() - INTERVAL '26 day'
    ),
    (
        'The Tower of Hanoi - Myths and Maths', 'Andreas M. Hinz', 'Công nghệ thông tin',
        'Khám phá bài toán Tháp Hà Nội dưới góc nhìn toán học và thuật toán.',
        1293522, 12, 'https://books.google.com/books/content?id=yKz0DwAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Birkhäuser', 'SEBOOK-DEMO-025', 2018, 'English',
        '["algorithm","math","hanoi"]'::jsonb, 450, 'Hardcover',
        830, 240, 170, 36, 1847888, 4.60, 7, NOW() - INTERVAL '2 day'
    ),
    (
        'The Economics of Science and Technology', 'M.P. Feldman', 'Kinh tế',
        'Tổng quan kinh tế học về đổi mới, khoa học và công nghệ.',
        2116808, 10, 'https://books.google.com/books/content?id=87qvQgAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Springer', 'SEBOOK-DEMO-026', 2001, 'English',
        '["economics","science","technology"]'::jsonb, 320, 'Hardcover',
        760, 240, 170, 30, 3024012, 4.10, 4, NOW() - INTERVAL '3 day'
    ),
    (
        'Behind the Gates', 'Eva Gray', 'Văn học',
        'Câu chuyện phiêu lưu dành cho độc giả trẻ trong thế giới nhiều bí mật.',
        150000, 44, 'https://books.google.com/books/content?id=xU9uBgAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'Scholastic', 'SEBOOK-DEMO-027', 2011, 'English',
        '["young adult","adventure","fiction"]'::jsonb, 224, 'Paperback',
        300, 200, 130, 18, 210000, 4.20, 12, NOW() - INTERVAL '1 day'
    ),
    (
        'Friends Learn Ballet', 'Janeen Brian', 'Thiếu nhi',
        'Một câu chuyện nhẹ nhàng về tình bạn, nghệ thuật và niềm vui học múa.',
        45000, 56, 'https://books.google.com/books/content?id=x3SFAAAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        'SEBook Demo', 'SEBOOK-DEMO-028', 2010, 'English',
        '["children","ballet","friendship"]'::jsonb, 32, 'Paperback',
        160, 210, 210, 6, 60000, 4.00, 5, NOW() - INTERVAL '5 day'
    );

INSERT INTO cat_book (
    title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
    publisher, isbn, publication_year, language, keywords, page_count, cover_type,
    weight, length, width, height, original_price, average_rating, rating_count, created_at, updated_at
)
SELECT
    title, author, description, price, deprecated_quantity, image_url, NULL, TRUE,
    publisher, isbn, publication_year, language, keywords, page_count, cover_type,
    weight, length, width, height, original_price, average_rating, rating_count, created_at, NOW()
FROM tmp_seed_books
ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE
SET title = EXCLUDED.title,
    author = EXCLUDED.author,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    deprecated_quantity = EXCLUDED.deprecated_quantity,
    image_url = EXCLUDED.image_url,
    is_active = TRUE,
    publisher = EXCLUDED.publisher,
    publication_year = EXCLUDED.publication_year,
    language = EXCLUDED.language,
    keywords = EXCLUDED.keywords,
    page_count = EXCLUDED.page_count,
    cover_type = EXCLUDED.cover_type,
    weight = EXCLUDED.weight,
    length = EXCLUDED.length,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    original_price = EXCLUDED.original_price,
    average_rating = EXCLUDED.average_rating,
    rating_count = EXCLUDED.rating_count,
    updated_at = NOW();

INSERT INTO cat_book_category (book_id, category_id)
SELECT book.id, category.id
FROM tmp_seed_books seed
JOIN cat_book book ON book.isbn = seed.isbn
JOIN cat_category category ON category.name = seed.category_name
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT book.id, seed.deprecated_quantity, 0, NOW(), NOW()
FROM tmp_seed_books seed
JOIN cat_book book ON book.isbn = seed.isbn
ON CONFLICT (book_id) DO UPDATE
SET quantity = EXCLUDED.quantity,
    updated_at = NOW();

COMMIT;

SELECT
    COUNT(*) AS seeded_books,
    SUM(CASE WHEN created_at >= NOW() - INTERVAL '7 day' THEN 1 ELSE 0 END) AS new_books_last_7_days
FROM cat_book
WHERE isbn LIKE 'SEBOOK-DEMO-%';
