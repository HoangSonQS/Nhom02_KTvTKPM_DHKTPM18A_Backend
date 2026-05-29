-- Dev seed data for local testing.
-- Safe to re-run: main records use ON CONFLICT where unique constraints exist.

INSERT INTO auth_user (email, password, full_name, role, enabled)
VALUES
    (
        'admin@sebook.com',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'System Administrator',
        'ADMIN',
        TRUE
    ),
    (
        'admin@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Admin SEBook Local',
        'ADMIN',
        TRUE
    ),
    (
        'staff.warehouse@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Warehouse Staff',
        'STAFF_WAREHOUSE',
        TRUE
    ),
    (
        'staff.seller@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Seller Staff',
        'STAFF_SELLER',
        TRUE
    ),
    (
        'customer.anna@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Nguyen An',
        'CUSTOMER',
        TRUE
    ),
    (
        'customer.binh@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Tran Binh',
        'CUSTOMER',
        TRUE
    ),
    (
        'customer.chi@sebook.local',
        '$2a$12$uInKvX9wwxornZEC4V0xCOImCypK3X3mqjthzVGE807ReOZGZ9/e.',
        'Le Chi',
        'CUSTOMER',
        TRUE
    )
ON CONFLICT (email) DO UPDATE
SET password = EXCLUDED.password,
    role = EXCLUDED.role,
    enabled = TRUE,
    full_name = EXCLUDED.full_name,
    updated_at = NOW();

INSERT INTO acc_account (user_id, created_at, updated_at)
SELECT u.id, NOW(), NOW()
FROM auth_user u
WHERE u.email IN (
    'admin@sebook.com',
    'admin@sebook.local',
    'staff.warehouse@sebook.local',
    'staff.seller@sebook.local',
    'customer.anna@sebook.local',
    'customer.binh@sebook.local',
    'customer.chi@sebook.local'
)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO cat_category (name, is_active)
VALUES
    ('Lap trinh', TRUE),
    ('Kinh doanh', TRUE),
    ('Thieu nhi', TRUE),
    ('Van hoc', TRUE)
ON CONFLICT (name) DO UPDATE
SET is_active = TRUE,
    updated_at = NOW();

INSERT INTO cat_book (
    title,
    author,
    description,
    price,
    deprecated_quantity,
    image_url,
    is_active,
    publisher,
    isbn,
    publication_year,
    language,
    keywords,
    page_count,
    cover_type,
    weight,
    length,
    width,
    height,
    original_price,
    average_rating,
    rating_count,
    created_at,
    updated_at
)
VALUES
    (
        'Clean Code - Ban thuc hanh',
        'Robert C. Martin',
        'Sach mau dung de test catalog va admin book CRUD.',
        189000,
        40,
        'https://images.unsplash.com/photo-1515879218367-8466d910aaa4',
        TRUE,
        'SEBook Demo',
        'DEV-SEBOOK-001',
        2024,
        'Vietnamese',
        '["lap trinh", "clean code", "software"]'::jsonb,
        420,
        'Paperback',
        550,
        24,
        16,
        3,
        230000,
        4.70,
        18,
        NOW(),
        NOW()
    ),
    (
        'Kinh doanh tinh gon',
        'Eric Ries',
        'Du lieu demo cho top sach va dashboard doanh thu.',
        159000,
        25,
        'https://images.unsplash.com/photo-1544947950-fa07a98d237f',
        TRUE,
        'SEBook Demo',
        'DEV-SEBOOK-002',
        2023,
        'Vietnamese',
        '["startup", "kinh doanh", "lean"]'::jsonb,
        320,
        'Paperback',
        430,
        21,
        14,
        2,
        199000,
        4.50,
        12,
        NOW(),
        NOW()
    ),
    (
        'Truyen ke truoc gio ngu',
        'SEBook Kids',
        'Sach demo cho danh muc thieu nhi.',
        79000,
        60,
        'https://images.unsplash.com/photo-1481627834876-b7833e8f5570',
        TRUE,
        'SEBook Demo',
        'DEV-SEBOOK-003',
        2025,
        'Vietnamese',
        '["thieu nhi", "truyen", "gia dinh"]'::jsonb,
        96,
        'Hardcover',
        300,
        20,
        20,
        1,
        99000,
        4.80,
        30,
        NOW(),
        NOW()
    )
ON CONFLICT (isbn) DO UPDATE
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
SELECT b.id, c.id
FROM cat_book b
JOIN cat_category c ON
    (b.isbn = 'DEV-SEBOOK-001' AND c.name = 'Lap trinh')
    OR (b.isbn = 'DEV-SEBOOK-002' AND c.name = 'Kinh doanh')
    OR (b.isbn = 'DEV-SEBOOK-003' AND c.name = 'Thieu nhi')
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, deprecated_quantity, 0, NOW(), NOW()
FROM cat_book
WHERE isbn IN ('DEV-SEBOOK-001', 'DEV-SEBOOK-002', 'DEV-SEBOOK-003')
ON CONFLICT (book_id) DO UPDATE
SET quantity = EXCLUDED.quantity,
    updated_at = NOW();

INSERT INTO prm_coupon (
    code,
    name,
    description,
    discount_type,
    discount_value,
    min_order_value,
    max_discount_value,
    usage_limit,
    used_count,
    start_date,
    end_date,
    is_active,
    version,
    created_at,
    updated_at
)
VALUES
    (
        'WELCOME10',
        'Welcome 10%',
        'Giam 10% cho don demo',
        'PERCENTAGE',
        10,
        100000,
        50000,
        100,
        0,
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '60 day',
        TRUE,
        0,
        NOW(),
        NOW()
    ),
    (
        'FREESHIP30',
        'Free Ship 30K',
        'Giam 30000 cho don tu 150000',
        'FIXED_AMOUNT',
        30000,
        150000,
        NULL,
        100,
        0,
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '60 day',
        TRUE,
        0,
        NOW(),
        NOW()
    )
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    min_order_value = EXCLUDED.min_order_value,
    max_discount_value = EXCLUDED.max_discount_value,
    usage_limit = EXCLUDED.usage_limit,
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO log_supplier (
    name,
    contact_person,
    phone_number,
    email,
    address,
    tax_code,
    created_at,
    updated_at
)
SELECT
    'SEBook Demo Supplier',
    'Nguyen Kho',
    '0900000001',
    'supplier@sebook.local',
    'Kho demo HCM',
    'DEMO-TAX-001',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM log_supplier
    WHERE email = 'supplier@sebook.local'
);

DELETE FROM pay_payment
WHERE transaction_id LIKE 'DEV-PAY-%';

DELETE FROM adm_order_report
WHERE order_id IN (
    SELECT id
    FROM ord_order
    WHERE request_id LIKE 'DEV-ORDER-%'
);

DELETE FROM ord_order_item
WHERE order_id IN (
    SELECT id
    FROM ord_order
    WHERE request_id LIKE 'DEV-ORDER-%'
);

DELETE FROM ord_order
WHERE request_id LIKE 'DEV-ORDER-%';

INSERT INTO ord_order (
    user_id,
    request_id,
    fulfillment_status,
    saga_status,
    total_amount,
    discount_amount,
    shipping_address,
    customer_phone,
    expired_at,
    created_at,
    updated_at
)
VALUES
    (
        (SELECT id FROM auth_user WHERE email = 'customer.anna@sebook.local'),
        'DEV-ORDER-001',
        'DELIVERED',
        'COMPLETED',
        537000,
        30000,
        '12 Nguyen Hue, Quan 1, TP.HCM',
        '0901000001',
        NOW() + INTERVAL '7 day',
        NOW() - INTERVAL '10 day',
        NOW() - INTERVAL '8 day'
    ),
    (
        (SELECT id FROM auth_user WHERE email = 'customer.binh@sebook.local'),
        'DEV-ORDER-002',
        'DELIVERING',
        'COMPLETED',
        318000,
        0,
        '25 Le Loi, Quan 3, TP.HCM',
        '0901000002',
        NOW() + INTERVAL '7 day',
        NOW() - INTERVAL '6 day',
        NOW() - INTERVAL '2 day'
    ),
    (
        (SELECT id FROM auth_user WHERE email = 'customer.chi@sebook.local'),
        'DEV-ORDER-003',
        'PROCESSING',
        'COMPLETED',
        268000,
        20000,
        '88 Dien Bien Phu, Binh Thanh, TP.HCM',
        '0901000003',
        NOW() + INTERVAL '7 day',
        NOW() - INTERVAL '4 day',
        NOW() - INTERVAL '1 day'
    ),
    (
        (SELECT id FROM auth_user WHERE email = 'customer.anna@sebook.local'),
        'DEV-ORDER-004',
        'CONFIRMED',
        'COMPLETED',
        159000,
        0,
        '12 Nguyen Hue, Quan 1, TP.HCM',
        '0901000001',
        NOW() + INTERVAL '7 day',
        NOW() - INTERVAL '2 day',
        NOW() - INTERVAL '2 day'
    ),
    (
        (SELECT id FROM auth_user WHERE email = 'customer.binh@sebook.local'),
        'DEV-ORDER-005',
        'CANCELLED',
        'COMPENSATED',
        189000,
        0,
        '25 Le Loi, Quan 3, TP.HCM',
        '0901000002',
        NOW() + INTERVAL '7 day',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day'
    );

INSERT INTO ord_order_item (order_id, book_id, book_title, price_at_purchase, quantity)
VALUES
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-001'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-001'), 'Clean Code - Ban thuc hanh', 189000, 2),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-001'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-002'), 'Kinh doanh tinh gon', 159000, 1),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-002'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-002'), 'Kinh doanh tinh gon', 159000, 2),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-003'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-001'), 'Clean Code - Ban thuc hanh', 189000, 1),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-003'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-003'), 'Truyen ke truoc gio ngu', 79000, 1),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-004'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-002'), 'Kinh doanh tinh gon', 159000, 1),
    ((SELECT id FROM ord_order WHERE request_id = 'DEV-ORDER-005'), (SELECT id FROM cat_book WHERE isbn = 'DEV-SEBOOK-001'), 'Clean Code - Ban thuc hanh', 189000, 1);

INSERT INTO pay_payment (
    order_id,
    transaction_id,
    amount,
    payment_method,
    status,
    result_data,
    created_at,
    updated_at
)
SELECT
    o.id,
    'DEV-PAY-' || RIGHT(o.request_id, 3),
    o.total_amount - COALESCE(o.discount_amount, 0),
    'VNPAY',
    CASE WHEN o.fulfillment_status = 'CANCELLED' THEN 'FAILED' ELSE 'SUCCESS' END,
    '{"source":"seed-dev-data"}',
    o.created_at + INTERVAL '10 minute',
    o.created_at + INTERVAL '12 minute'
FROM ord_order o
WHERE o.request_id LIKE 'DEV-ORDER-%';

INSERT INTO adm_order_report (
    order_id,
    customer_name,
    total_amount,
    status,
    coupon_code,
    items_summary,
    payment_method,
    cancellation_reason,
    created_at,
    checkout_at,
    paid_at,
    completed_at,
    refund_amount,
    refunded_at,
    updated_at
)
SELECT
    o.id,
    u.full_name,
    o.total_amount - COALESCE(o.discount_amount, 0),
    o.fulfillment_status,
    CASE WHEN o.discount_amount > 0 THEN 'WELCOME10' ELSE NULL END,
    (
        SELECT string_agg(i.book_title || ' x' || i.quantity, ', ' ORDER BY i.id)
        FROM ord_order_item i
        WHERE i.order_id = o.id
    ),
    CASE WHEN o.fulfillment_status = 'CANCELLED' THEN NULL ELSE 'VNPAY' END,
    CASE WHEN o.fulfillment_status = 'CANCELLED' THEN 'Khach huy don demo' ELSE NULL END,
    o.created_at,
    o.created_at,
    CASE WHEN o.fulfillment_status = 'CANCELLED' THEN NULL ELSE o.created_at + INTERVAL '10 minute' END,
    CASE WHEN o.fulfillment_status = 'DELIVERED' THEN o.updated_at ELSE NULL END,
    CASE WHEN o.request_id = 'DEV-ORDER-001' THEN 79000 ELSE NULL END,
    CASE WHEN o.request_id = 'DEV-ORDER-001' THEN o.updated_at + INTERVAL '1 day' ELSE NULL END,
    o.updated_at
FROM ord_order o
JOIN auth_user u ON u.id = o.user_id
WHERE o.request_id LIKE 'DEV-ORDER-%'
ON CONFLICT (order_id) DO UPDATE
SET customer_name = EXCLUDED.customer_name,
    total_amount = EXCLUDED.total_amount,
    status = EXCLUDED.status,
    coupon_code = EXCLUDED.coupon_code,
    items_summary = EXCLUDED.items_summary,
    payment_method = EXCLUDED.payment_method,
    cancellation_reason = EXCLUDED.cancellation_reason,
    checkout_at = EXCLUDED.checkout_at,
    paid_at = EXCLUDED.paid_at,
    completed_at = EXCLUDED.completed_at,
    refund_amount = EXCLUDED.refund_amount,
    refunded_at = EXCLUDED.refunded_at,
    updated_at = NOW();

SELECT
    (SELECT COUNT(*) FROM auth_user WHERE role = 'ADMIN') AS admin_count,
    (SELECT COUNT(*) FROM auth_user WHERE role = 'CUSTOMER') AS customer_count,
    (SELECT COUNT(*) FROM cat_book WHERE isbn LIKE 'DEV-SEBOOK-%') AS demo_books,
    (SELECT COUNT(*) FROM ord_order WHERE request_id LIKE 'DEV-ORDER-%') AS demo_orders,
    (SELECT COUNT(*) FROM inv_stock) AS stock_rows,
    (SELECT COUNT(*) FROM prm_coupon WHERE code IN ('WELCOME10', 'FREESHIP30')) AS demo_coupons,
    (SELECT COUNT(*) FROM log_supplier) AS suppliers;
