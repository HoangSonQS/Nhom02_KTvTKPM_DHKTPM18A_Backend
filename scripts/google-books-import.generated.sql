BEGIN;
INSERT INTO cat_category (name, is_active, created_at) VALUES (U&'V\0103n h\1ECDc', TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Journey to the Center of the Earth',
        U&'Jules Verne',
        U&'The intrepid Professor Lindenbrock embarks upon the strangest expedition of the nineteenth century: a journey down an extinct Icelandic volcano to the Earth\2019s very core. In his quest to penetrate the planet\2019s primordial secrets, the geologist\2014together with his quaking nephew Axel and their devoted guide, Hans\2014discovers an astonishing subterranean menagerie of prehistoric proportions. Verne\2019s imaginative tale is at once the ultimate science fiction adventure and a reflection on the perfectibility of human understanding and the psychology of the questor. As David Brin notes in his Introduction, though Verne never knew the term \201Cscience fiction,\201D Journey to the Centre of the Earth is \201Cinarguably one of the wellsprings from which it all began.\201D',
        55949,
        51,
        U&'https://books.google.com/books/content?id=icKmd-tlvPMC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Bantam',
        U&'9780553902549',
        2006,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        242,
        U&'B\00ECa m\1EC1m',
        67409,
        3.5,
        3,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780553902549'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 51, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780553902549'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Mysteries',
        U&'Knut Hamsun',
        U&'The first complete English translation of the Nobel Prize-winner\2019s literary masterpiece A Penguin Classic Mysteries is the story of Johan Nilsen Nagel, a mysterious stranger who suddenly turns up in a small Norwegian town one summer\2014and just as suddenly disappears. Nagel is a complete outsider, a sort of modern Christ treated in a spirit of near parody. He condemns the politics and thought of the age, brings comfort to the \201Cinsulted and injured,\201D and gains the love of two women suggestive of the biblical Mary and Martha. But there is a sinister side of him: in his vest he carries a vial of prussic acid... The novel creates a powerful sense of Nagel''s stream of thought, as he increasingly withdraws into the torture chamber of his own subconscious psyche. For more than seventy years, Penguin has been the leading publisher of classic literature in the English-speaking world. With more than 1,800 titles, Penguin Classics represents a global bookshelf of the best works throughout history and across genres and disciplines. Readers trust the series to provide authoritative texts enhanced by introductions and notes by distinguished scholars and contemporary authors, as well as up-to-date translations by award-winning translators.',
        160000,
        56,
        U&'https://books.google.com/books/content?id=MRoMUV2kLZEC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Penguin',
        U&'9780141186184',
        2001,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        356,
        U&'B\00ECa m\1EC1m',
        192000,
        4,
        3,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780141186184'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 56, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780141186184'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Taras Bulba',
        U&'Nikol\00E1i V. Gogol',
        U&'Feroces, crueles, valientes y apasionados, los cosacos hacen temblar la estepa bajo los cascos de sus caballos. Y entre ellos se encuentra Taras Bulba, un anciano lleno a\00FAn de fuerza e inteligencia que junto a sus hijos, Ostap y Andr\00ED, avanzar\00E1 por tierras polacas con intenci\00F3n de vengar su fe ortodoxa burlada por los cat\00F3licos. Ninguna guarnici\00F3n, ciudad amurallada o iglesia podr\00E1n detenerlos, hasta que la desgracia se cierna sobre ellos y el apuesto y enamoradizo Andr\00ED haga que su padre maldiga el d\00EDa en que lo engendr\00F3. Taras Bulba, una anomal\00EDa entre la obra m\00E1s conocida de Gogol, es una aventura trepidante, una sinfon\00EDa en perpetuo crescendo, en la que cada cap\00EDtulo es m\00E1s intenso y sorprendente que el anterior. un fresco tan afinadamente dibujado y tan v\00EDvido que resulta absolutamente intemporal.',
        69000,
        41,
        U&'https://books.google.com/books/content?id=XdMBTKWSfeMC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Ediciones AKAL',
        U&'9788446023708',
        2006,
        U&'es',
        U&'["V\0103n h\1ECDc","Literary Criticism"]'::jsonb,
        154,
        U&'B\00ECa m\1EC1m',
        83000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9788446023708'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 41, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9788446023708'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Frankenstein',
        U&'Mary Shelley',
        U&'Frankenstein; or, The Modern Prometheus is an 1818 Gothic novel written by English author Mary Shelley. Frankenstein tells the story of Victor Frankenstein, a young scientist who creates a sapient creature in an unorthodox scientific experiment that involved putting it together with different body parts. Shelley started writing the story when she was 18 and staying in Bath, and the first edition was published anonymously in London on 1 January 1818, when she was 20. Her name first appeared in the second edition, which was published in Paris in 1821. Shelley travelled through Europe in 1815, moving along the river Rhine in Germany, and stopping in Gernsheim, 17 kilometres (11 mi) away from Frankenstein Castle, where, about a century earlier, Johann Konrad Dippel, an alchemist, had engaged in experiments. She then journeyed to the region of Geneva, Switzerland, where much of the story takes place. Galvanism and occult ideas were topics of conversation for her companions, particularly for her lover and future husband Percy Bysshe Shelley. In 1816, Mary, Percy, John Polidori, and Lord Byron had a competition to see who would write the best horror story.',
        100000,
        28,
        U&'https://books.google.com/books/content?id=lyWOEQAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Seven Books',
        U&'9789403837420',
        2025,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        222,
        U&'B\00ECa m\1EC1m',
        120000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9789403837420'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 28, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9789403837420'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Odyssey of Homer',
        U&'Homer',
        U&'A stunning verse translation of Homer\2019s epic tale about one man\2019s decade-long struggle to return home after years on the battlefields of Troy, from National Book Award\2013winning translator Allen Mandelbaum The inspiration for the upcoming film The Odyssey, directed by Christopher Nolan and starring Matt Damon, Tom Holland, Elliot Page, and Zendaya \201CMuse, tell me of the man of many wiles . . .\201D So begins one of the greatest adventures of world literature. Homer\2019s epic chronicle of the Greek hero Odysseus\2019 journey home from the Trojan War has inspired writers from Virgil to James Joyce. Odysseus survives storm and shipwreck, the cave of the Cyclops and the isle of Circe, the lure of the Sirens\2019 song and a trip to the Underworld, only to find his most difficult challenge at home, where treacherous suitors seek to steal his kingdom and his loyal wife, Penelope. Allen Mandelbaum\2019s brilliant verse translation realizes the power and the beauty of the original Greek and demonstrates why the Odyssey has captured the human imagination for nearly three thousand years.',
        55949,
        73,
        U&'https://books.google.com/books/content?id=ORyo8qAA-CQC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Bantam Classics',
        U&'9780553897777',
        2005,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        559,
        U&'B\00ECa m\1EC1m',
        67409,
        5,
        1,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780553897777'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 73, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780553897777'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Last Man',
        U&'Mary Shelley',
        U&'First published in 1826, The Last Man is an apocalyptic sci-ence fiction work by Mary Shelley. A courtly society at the crossroads between monarchy and republic is afflicted by a serious epidemic that threatens the survival of all mankind. Will Lionel be able to save the few who are faithful to him? In addition to an exciting novel, Shelley also succeeds in in-terweaving autobiographical, historical and scientific aspects into the story.',
        239000,
        52,
        U&'https://books.google.com/books/content?id=5nNpEAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'epubli',
        U&'9783750242494',
        2019,
        U&'de',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        532,
        U&'B\00ECa m\1EC1m',
        287000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9783750242494'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 52, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9783750242494'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Oliver Twist',
        U&'Charles Dickens',
        U&'JAICO ILLUSTARTED CLASSICS SERIES is a collection of beloved children\2019s classics read by generations all over the world. Rich with adventures and thrills, these immortal stories with vivid illustrations are designed to delight young readers. IN A RAMSHACKLE workhouse of a parish in England, a baby boy was born to a woman who died in childbirth. The boy was named OLIVER TWIST. He spent a sad childhood in abject misery in various workhouses. At the age of twelve, he runs away to London where he is picked up by a notorious criminal, Fagin, a mastermind at house breaking, pickpocketing and petty thievery. He teaches Oliver all these evil deeds. But a kind-hearted gentleman robbed by Fagin sympathizes with the plight of Oliver and helps him to find a family, his inheritance and above all Oliver\2019s true identity. CHARLES DICKENS was born in a little house in Landport, Portsea, England. The second of eight children, he grew up in a family frequently beset by financial insecurity. When the family fortunes improved, Dickens went back to school, after which he became an office boy, a freelance reporter, and finally an author.',
        99000,
        57,
        U&'https://books.google.com/books/content?id=3oSqDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Jaico Publishing House',
        U&'9789388423038',
        2019,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        220,
        U&'B\00ECa m\1EC1m',
        119000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9789388423038'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 57, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9789388423038'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Street of Crocodiles',
        U&'Bruno Schulz',
        U&'''''The Street of Crocodiles'''' by Bruno Schulz (1892-1942) was first published in Polish in 1934; this English translation was first published in the US by Walker and Company in 1963, public domain. A novel that blends the real and the fantastic, from "one of the most original imaginations in modern Europe" (Cynthia Ozick). The Street of Crocodiles in the Polish city of Drogobych is a street of memories and dreams where recollections of Bruno Schulz''s uncommon boyhood and of the eerie side of his merchant family''s life are evoked in a startling blend of the real and the fantastic. Most memorable - and most chilling - is the portrait of the author''s father, a maddened shopkeeper who imports rare birds'' eggs to hatch in his attic, who believes tailors'' dummies should be treated like people, and whose obsessive fear of cockroaches causes him to resemble one. Bruno Schulz, a Polish Jew killed by the Nazis in 1942, is considered by many to have been the leading Polish writer between the two world wars.',
        61000,
        31,
        U&'https://books.google.com/books/content?id=v7VPEQAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Rare Treasure Editions',
        U&'9788087830277',
        2025,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        136,
        U&'B\00ECa m\1EC1m',
        73000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9788087830277'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 31, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9788087830277'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Picture of Dorian Gray',
        U&'Oscar Wilde',
        U&'The Picture of Dorian Gray is the only published novel by Oscar Wilde, appearing as the lead story in Lippincott''s Monthly Magazine on 20 June 1890, printed as the July 1890 issue. The magazine''s editors feared the story was indecent as submitted, so they censored roughly 500 words, without Wilde''s knowledge, before publication. But even with that, the story was still greeted with outrage by British reviewers, some of whom suggested that Wilde should be prosecuted on moral grounds, leading Wilde to defend the novel aggressively in letters to the British press. Today, Wilde''s fin de si\00E8cle novella is considered a classic. This new edition from Immortal Books includes footnotes and images.',
        85000,
        35,
        U&'https://books.google.com/books/content?id=6vGiDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Lulu.com',
        U&'9780359788330',
        2019,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        188,
        U&'B\00ECa m\1EC1m',
        102000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780359788330'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 35, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780359788330'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'V\0103n h\1ECDc'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Tess of the D''Urbervilles',
        U&'Thomas Hardy',
        U&'Story of Tess Durbeyfield, the daughter of a poor and dissipated villager.',
        175000,
        52,
        U&'https://books.google.com/books/content?id=U-1LyAaWtCoC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Wordsworth Editions',
        U&'9781853260056',
        1992,
        U&'en',
        U&'["V\0103n h\1ECDc","Fiction"]'::jsonb,
        388,
        U&'B\00ECa m\1EC1m',
        210000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781853260056'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 52, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781853260056'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
INSERT INTO cat_category (name, is_active, created_at) VALUES (U&'Kinh t\1EBF', TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Economics of Science and Technology',
        U&'M.P. Feldman',
        U&'Science and technology have long been regarded as important determinants of economic growth. Edwin Mansfield (1971, pp. 1- 2), a pioneer in the economics of technological change, noted: Technological change is an important, if not the most important, factor responsible for economic growth . . . without question, [it] is one of the most important determinants of the shape and evolution of the American economy. Science and technology are even more important in the "new economy," with its greater emphasis on the role of intellectual property and knowledge transfer. Therefore, it is unfortunate that most individuals rarely have the opportunity to explore the economic implications of science and technology. As a result, the antecedents and consequences of technological change are poorly understood by many in the general public. This lack of understanding is reflected in a recent survey conducted by the National Science Board (2000), summarized in Science & Engineering Indicators. '' As shown in Table 1. 1, the findings of the survey indicated that many Americans, despite a high level of interests in such matters, are not as well-informed about technological issues as they are about other policy issues. As shown in the table, individuals self assess, based on a scale from 1 to 100, their interest in science and technology policy issues as being relatively high, yet they self assess their knowledge or informedness about these issues relatively lower.',
        2116808,
        41,
        U&'https://books.google.com/books/content?id=wca9BwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Springer Science & Business Media',
        U&'9781461509813',
        2012,
        U&'en',
        U&'["Kinh t\1EBF","Business \005Cu0026 Economics"]'::jsonb,
        132,
        U&'B\00ECa m\1EC1m',
        3024012,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781461509813'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 41, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781461509813'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Business',
        U&'Mark Vernon',
        U&'"Each entry begins with a definition of the term followed by an insightful description and discussion of the concept that is designed both to ground the subject firmly and outline engaging avenues of developments. The entries conclude with helpful cross-referencing and suggested further reading." "Business: The Key Concepts is a desktop resource for anyone who wants to learn more bout the commercial world that surrounds them."--BOOK JACKET.',
        114000,
        56,
        U&'https://books.google.com/books/content?id=HYdxEW-J4RoC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Psychology Press',
        U&'9780415253239',
        2002,
        U&'en',
        U&'["Kinh t\1EBF","Business \005Cu0026 Economics"]'::jsonb,
        253,
        U&'B\00ECa m\1EC1m',
        137000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780415253239'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 56, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780415253239'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Doing Business with Lithuania',
        U&'Marat Terterov',
        U&'Originally published in the pre-EU-accession period, this E-Book edition of Doing Business with Lithuania has been updated to take account of the post-accession changes to the legal and fiscal environment. It remains a definitive appraisal of the economic system and investment climate, including an examination of the legal structure and business regulation, information on the financial sector and unique best practice on all aspects of trading with and investing in Lithuania. The guide also provides an overview of key sectors of trade and investment.',
        165000,
        71,
        U&'https://books.google.com/books/content?id=0KWJf6mVorEC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'GMB Publishing Ltd',
        U&'9781905050628',
        2005,
        U&'en',
        U&'["Kinh t\1EBF","Business \005Cu0026 Economics"]'::jsonb,
        366,
        U&'B\00ECa m\1EC1m',
        198000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781905050628'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 71, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781905050628'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Accounting for Sustainability',
        U&'Anthony G. Hopwood',
        U&'If businesses and other organizations are to meet the many and complex challenges of sustainable development, then they all, both public and private, need to embed sustainability considerations into their decision-making and reporting. However, the translation of this aspiration into effective action is often inhibited by the lack of systems and procedures that take sustainability into account.Accounting for Sustainability: Practical Insights will help organizations to address these issues. The book sets out a number of tools and approaches that have been developed and applied by leading organizations to:- embed sustainability into decision-making, extending beyond an organization''s boundaries to take into account suppliers, customers and other stakeholders;- measure and link sustainability and financial performance;- integrate sustainability into ''mainstream'' reporting, both to management and external stakeholders.In-depth cases studies from Aviva, BT, the Environment Agency, EDF Energy, HSBC, Novo Nordisk, Sainsbury''s and West Sussex County Council show in detail how accounting for sustainability works in practice in a wide range of organizational contexts.Published with The Prince''s Charities: Accounting for Sustainability',
        129000,
        64,
        U&'https://books.google.com/books/content?id=2OhgnuzJCk4C&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Earthscan',
        U&'9781849776332',
        2010,
        U&'en',
        U&'["Kinh t\1EBF","Business \005Cu0026 Economics"]'::jsonb,
        286,
        U&'B\00ECa m\1EC1m',
        155000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781849776332'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 64, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781849776332'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'A New Vision, a New Heart, a Renewed Call',
        U&'David Claydon',
        NULL,
        260000,
        51,
        U&'https://books.google.com/books/content?id=RfGhUW8RdUIC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'William Carey Library',
        U&'9780878083640',
        2005,
        U&'en',
        U&'["Kinh t\1EBF","Religion"]'::jsonb,
        722,
        U&'B\00ECa m\1EC1m',
        312000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780878083640'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 51, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780878083640'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'La meta',
        U&'Eliyahu M. Goldratt',
        U&'La meta es una novela \00E1gil y cautivante, que transcurre en el mundo de los negocios, a trav\00E9s de una entretenida narraci\00F3n describe un m\00E9todo infalible para mejorar los resultados de la empresa. En esta obra se desarrolla',
        199000,
        56,
        U&'https://books.google.com/books/content?id=obXz7Rn0N8cC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'North River Press',
        U&'9780884271642',
        1999,
        U&'es',
        U&'["Kinh t\1EBF","Business \005Cu0026 Economics"]'::jsonb,
        442,
        U&'B\00ECa m\1EC1m',
        239000,
        4,
        12,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780884271642'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 56, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780884271642'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Research Methods for Business Students',
        U&'M. N. K. Saunders',
        U&'"This accessible and clearly written textbook provides a comprehensive and in-depth treatment of philosophical, methodological and ethical aspects of conducting business and management research. Illustrative case studies drawing on published research studies are used throughout and readers are given multiple opportunities to consolidate their learning through review and discussion questions, quizzes, and other exercises. At the end of each chapter a case study takes the reader through the realities and practicalities of applying the knowledge to a specific student research project. This will be an invaluable guide for all students seeking to understand and undertake business and management research." Professor Natasha Mauthner, Newcastle University With over 400,000 copies sold, Research Methods for Business Students, is the definitive and market-leading textbook for Business and Management students conducting a research-led project or dissertation. The fully revised 8th edition answers key questions such as: How do I choose my topic and design the research? Why is research philosophy relevant to my research? How do I collect and analyse my data? When and what do I need to write? With the 8th edition you will discover: \00FC Fully updated chapters incorporating visual methods throughout, detailed insights on drafting the critical literature review, the latest EU data protection regulations, using audio recordings and visual images in observation research, collecting data using diaries, the use of online survey tools, and preparing and presenting an academic poster \00FC New cases using up-to-date scenarios at the end of each chapter \00FC Boxed examples throughout of research methods in the news, from student research and in published management research \00FC A glossary of clear definitions of over 700 research-related terms \00FC Practical guidance and opportunities for checking your learning and self-reflection to enable you to progress your own research \00FC Detailed chapters on choosing your topic, critically reviewing the literature, understanding philosophies, research design, access and ethics, secondary data, data collection methods and analysis techniques and writing about and presenting your research \00FC Teach yourself guides to research software available at www.pearsoned.co.uk/saunders with practice data sets About the authors Mark NK Saunders is Professor of Business Research Methods and Director of Postgraduate Research Programmes at Birmingham Business School, University of Birmingham. Philip Lewis was a Principal Lecturer and Adrian Thornhill was a Head of Department, both at the University of Gloucestershire.',
        260000,
        30,
        U&'https://books.google.com/books/content?id=LtiQvwEACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        NULL,
        U&'9781292208787',
        2019,
        U&'en',
        U&'["Kinh t\1EBF","Business"]'::jsonb,
        864,
        U&'B\00ECa m\1EC1m',
        312000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781292208787'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 30, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781292208787'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Kinh t\1EBF'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Environmental Planning and Management',
        U&'Christian N. Madu',
        U&'This book focuses on environmental planning and management. Environmental problems are not purely scientific; some of the major problems deal with poor management and the inability to involve people in environmental decision making process. The approach taken in this book is to review environmental problems as they are affected by poor planning and management. Understanding of management issues involved will help to get top management to buy into environmental management. The tendency is for top management to view environmental management efforts as expensive and wasteful to an organization. However, when top management is exposed to the high cost of doing nothing and the lack of competitiveness as a result of poor environmental quality, it is more likely to buy into the idea of environmental quality and work towards achieving sustainable goals.',
        115000,
        27,
        U&'https://books.google.com/books/content?id=5a1gDQAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Imperial College Press',
        U&'9781860947988',
        2007,
        U&'en',
        U&'["Kinh t\1EBF","Political Science"]'::jsonb,
        255,
        U&'B\00ECa m\1EC1m',
        138000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781860947988'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 27, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781860947988'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
INSERT INTO cat_category (name, is_active, created_at) VALUES (U&'K\1EF9 n\0103ng s\1ED1ng', TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'K\1EF9 n\0103ng s\1ED1ng'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Empowerment of Women Through Self Help Groups',
        U&'G. Sreeramulu',
        U&'The book is an in-depth study of Empowerment of Women Through Self Help Groups. It covers the problems and perspectives of Self Help Groups and suggest several measures. The study has evaluated the implementation of several schemes in Anantapur District in particular and in Andhra Pradesh in general such as rearing goats, dairying, petty business activities, making of soft toys and so on. The findings are very much encouraging, such as Women are now managing their families, Panchayat Raj Institutions, are able to concentrate on their children s education and health. Contents include: Introduction, Public Policy Theoretical Perspectives, Evaluation, Aims and Objectctives of Self Help Groups in Anantapur District, Socio-Economic Background of the Sample Study, Problems and perspectives of Self Help Groups, Performance of Self Help Groups and Conclusion. This outstanding Text-cum-Reference book will be of great use to Scholars, Administrators, Planners, Policy-makers, Statesmen and Students of Political Science, Economics, Sociology, Commerce and Women Studites.',
        150000,
        54,
        U&'https://books.google.com/books/content?id=Mmtn9-YpF6EC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Gyan Books',
        U&'9788178355016',
        2006,
        U&'en',
        U&'["K\1EF9 n\0103ng s\1ED1ng","Political Science"]'::jsonb,
        334,
        U&'B\00ECa m\1EC1m',
        180000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9788178355016'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 54, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9788178355016'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'K\1EF9 n\0103ng s\1ED1ng'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Self and Family',
        U&'Jane Cary Peck',
        NULL,
        56000,
        17,
        U&'https://books.google.com/books/content?id=45jlDZlhT4sC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Westminster John Knox Press',
        U&'9780664245474',
        1984,
        U&'en',
        U&'["K\1EF9 n\0103ng s\1ED1ng","Family \005Cu0026 Relationships"]'::jsonb,
        124,
        U&'B\00ECa m\1EC1m',
        67000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780664245474'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 17, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780664245474'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
INSERT INTO cat_category (name, is_active, created_at) VALUES (U&'Thi\1EBFu nhi', TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Friends Learn Ballet',
        U&'Janeen Brian',
        U&'Ellie practices ballet steps with her friend Natalie and both dream of becoming ballet dancers. Includes a vocabulary list, discussion questions, and a note for grownups.',
        45000,
        56,
        U&'https://books.google.com/books/content?id=sEAZiIiZEJYC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        NULL,
        U&'9780918831187',
        1985,
        U&'en',
        U&'["Thi\1EBFu nhi","Juvenile Nonfiction"]'::jsonb,
        36,
        U&'B\00ECa m\1EC1m',
        54000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780918831187'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 56, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780918831187'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Silver Door',
        U&'Holly Lisle',
        U&'When Genna is chosen as the Sunrider of prophecy, her destiny is to unite the magic of the sun and the moon for the good of both Nightlings and humans.',
        174000,
        48,
        U&'https://books.google.com/books/content?id=Sj5MWqEPsDkC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Scholastic Inc.',
        U&'9780545000154',
        2010,
        U&'en',
        U&'["Thi\1EBFu nhi","Juvenile Fiction"]'::jsonb,
        386,
        U&'B\00ECa m\1EC1m',
        209000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780545000154'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 48, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780545000154'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Henry''s Leg',
        U&'Ann Pilling',
        NULL,
        71000,
        36,
        U&'https://books.google.com/books/content?id=DYWzQAAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        NULL,
        U&'9780140320169',
        1986,
        U&'en',
        U&'["Thi\1EBFu nhi","Children of divorced parents"]'::jsonb,
        158,
        U&'B\00ECa m\1EC1m',
        85000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780140320169'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 36, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780140320169'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Bad Magic',
        U&'Pseudonymous Bosch',
        U&'Thirteen-year-old Clay, a boy who no longer believes in magic, tags graffiti on his classroom wall and, as punishment, is sent to a camp for wayward kids located on a volcanic island, where eccentric campmates abound, a ghost walks among the abandoned ruins of a mansion, and a dangerous force threatens to erupt with bad magic.',
        139000,
        32,
        U&'https://books.google.com/books/content?id=leRlrgEACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        NULL,
        U&'9780545850148',
        2014,
        U&'en',
        U&'["Thi\1EBFu nhi","Camps"]'::jsonb,
        0,
        U&'B\00ECa m\1EC1m',
        167000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780545850148'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 32, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780545850148'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Behind the Gates',
        U&'Eva Gray',
        U&'In a Chicago troubled by the war against the Alliance, Louisa and her best friend, Maddie, disguised as her twin sister, are sent to the exclusive Country Manor School, where they are cut off from the outside world and learn survival skills.',
        150000,
        46,
        U&'https://books.google.com/books/content?id=HEmJngEACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Scholastic Paperbacks',
        U&'9780545317016',
        2011,
        U&'en',
        U&'["Thi\1EBFu nhi","Boarding schools"]'::jsonb,
        0,
        U&'B\00ECa m\1EC1m',
        180000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780545317016'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 46, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780545317016'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'School Days',
        U&'Laura Ingalls Wilder',
        U&'Laura and her sisters share some good and bad times when they attend different schools near their various prairie homes.',
        45000,
        22,
        U&'https://books.google.com/books/content?id=qGsZxfIV49wC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        NULL,
        U&'9780590189057',
        1997,
        U&'en',
        U&'["Thi\1EBFu nhi","Frontier and pioneer life"]'::jsonb,
        84,
        U&'B\00ECa m\1EC1m',
        54000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780590189057'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 22, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780590189057'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Keep of the Ancient King; Illus by Michael Fishel; Cover Art by Keith Parkinson',
        U&'Mike Carr',
        NULL,
        45000,
        58,
        NULL,
        NULL,
        TRUE,
        NULL,
        U&'9780880380621',
        1983,
        U&'en',
        U&'["Thi\1EBFu nhi","Good and evil"]'::jsonb,
        76,
        U&'B\00ECa m\1EC1m',
        54000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780880380621'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 58, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780880380621'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Caring Animals',
        U&'Rosanna Hansen',
        U&'Ideal for today''s young investigative reader, each A True Book includes lively sidebars, a glossary and index, plus a comprehensive "To Find Out More" section listing books, organizations, and Internet sites. A staple of library collections since the 1950s, the new A True Book series is the definitive nonfiction series for elementary school readers.',
        45000,
        53,
        U&'https://books.google.com/books/content?id=RlLtaAtfKrEC&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Children''s Press(CT)',
        U&'9780516229126',
        2003,
        U&'en',
        U&'["Thi\1EBFu nhi","Juvenile Nonfiction"]'::jsonb,
        56,
        U&'B\00ECa m\1EC1m',
        54000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780516229126'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 53, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780516229126'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Spring Is Here',
        U&'Houghton Mifflin Company',
        U&'Follows the four seasons around the year, from snow melting into spring, through the quiet harvest and the fall of snow, and then to spring again.',
        45000,
        69,
        U&'https://books.google.com/books/content?id=zlkJYAAACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Houghton Mifflin School',
        U&'9780618036516',
        2000,
        U&'en',
        U&'["Thi\1EBFu nhi","Juvenile Nonfiction"]'::jsonb,
        48,
        U&'B\00ECa m\1EC1m',
        54000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780618036516'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 69, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780618036516'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'You Wouldn''t Want to Live Without Soap!',
        U&'Alex Woolf',
        U&'For use in schools and libraries only. Would you rather put your grubby clothes in the washing machine, or take them down to the river and beat the dirt out of them? You re lucky to have the choice! Soaps and detergents are among the great benefits of modern life. They help to keep us comfort',
        143000,
        12,
        U&'https://books.google.com/books/content?id=NUw4jgEACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api',
        NULL,
        TRUE,
        U&'Turtleback Books',
        U&'9780606374736',
        2015,
        U&'en',
        U&'["Thi\1EBFu nhi","JUVENILE NONFICTION"]'::jsonb,
        0,
        U&'B\00ECa m\1EC1m',
        172000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780606374736'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 12, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780606374736'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'Thi\1EBFu nhi'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Cleopatra VII',
        U&'Kristiana Gregory',
        U&'While her father is in hiding after attempts on his life, twelve-year-old Cleopatra records in her diary how she fears for her own safety and hopes to survive to become Queen of Egypt some day.',
        99000,
        16,
        NULL,
        NULL,
        TRUE,
        NULL,
        U&'9780439320443',
        1999,
        U&'en',
        U&'["Thi\1EBFu nhi","Cleopatra"]'::jsonb,
        221,
        U&'B\00ECa m\1EC1m',
        119000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780439320443'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 16, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780439320443'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
INSERT INTO cat_category (name, is_active, created_at) VALUES (U&'C\00F4ng ngh\1EC7 th\00F4ng tin', TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Pragmatic Programmer',
        U&'Andrew Hunt',
        U&'What others in the trenches say about The Pragmatic Programmer... \201CThe cool thing about this book is that it\2019s great for keeping the programming process fresh. The book helps you to continue to grow and clearly comes from people who have been there.\201D \2014 Kent Beck, author of Extreme Programming Explained: Embrace Change \201CI found this book to be a great mix of solid advice and wonderful analogies!\201D \2014 Martin Fowler, author of Refactoring and UML Distilled \201CI would buy a copy, read it twice, then tell all my colleagues to run out and grab a copy. This is a book I would never loan because I would worry about it being lost.\201D \2014 Kevin Ruland, Management Science, MSG-Logistics \201CThe wisdom and practical experience of the authors is obvious. The topics presented are relevant and useful.... By far its greatest strength for me has been the outstanding analogies\2014tracer bullets, broken windows, and the fabulous helicopter-based explanation of the need for orthogonality, especially in a crisis situation. I have little doubt that this book will eventually become an excellent source of useful information for journeymen programmers and expert mentors alike.\201D \2014 John Lakos, author of Large-Scale C++ Software Design \201CThis is the sort of book I will buy a dozen copies of when it comes out so I can give it to my clients.\201D \2014 Eric Vought, Software Engineer \201CMost modern books on software development fail to cover the basics of what makes a great software developer, instead spending their time on syntax or technology where in reality the greatest leverage possible for any software team is in having talented developers who really know their craft well. An excellent book.\201D \2014 Pete McBreen, Independent Consultant \201CSince reading this book, I have implemented many of the practical suggestions and tips it contains. Across the board, they have saved my company time and money while helping me get my job done quicker! This should be a desktop reference for everyone who works with code for a living.\201D \2014 Jared Richardson, Senior Software Developer, iRenaissance, Inc. \201CI would like to see this issued to every new employee at my company....\201D \2014 Chris Cleeland, Senior Software Engineer, Object Computing, Inc. \201CIf I\2019m putting together a project, it\2019s the authors of this book that I want. . . . And failing that I\2019d settle for people who\2019ve read their book.\201D \2014 Ward Cunningham Straight from the programming trenches, The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development to examine the core process--taking a requirement and producing working, maintainable code that delights its users. It covers topics ranging from personal responsibility and career development to architectural techniques for keeping your code flexible and easy to adapt and reuse. Read this book, and you''ll learn how to Fight software rot; Avoid the trap of duplicating knowledge; Write flexible, dynamic, and adaptable code; Avoid programming by coincidence; Bullet-proof your code with contracts, assertions, and exceptions; Capture real requirements; Test ruthlessly and effectively; Delight your users; Build teams of pragmatic programmers; and Make your developments more precise with automation. Written as a series of self-contained sections and filled with entertaining anecdotes, thoughtful examples, and interesting analogies, The Pragmatic Programmer illustrates the best practices and major pitfalls of many different aspects of software development. Whether you''re a new coder, an experienced programmer, or a manager responsible for software projects, use these lessons daily, and you''ll quickly see improvements in personal productivity, accuracy, and job satisfaction. You''ll learn skills and develop habits and attitudes that form the foundation for long-term success in your career. You''ll become a Pragmatic Programmer.',
        601002,
        51,
        U&'https://books.google.com/books/content?id=5wBQEp6ruIAC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Addison-Wesley Professional',
        U&'9780132119177',
        1999,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Computers"]'::jsonb,
        346,
        U&'B\00ECa m\1EC1m',
        858574,
        4.5,
        7,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780132119177'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 51, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780132119177'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Lady from the Sea',
        U&'Henrik Ibsen',
        U&'The Lady from the Sea" is a play written by Henrik Ibsen, first performed in 1888. Set in a small Norwegian coastal town, the play explores themes of duty, freedom, and personal fulfillment. The story revolves around Ellida Wangel, the lady of the sea, who is trapped in a marriage to Dr. Wangel, a widower with two daughters. Ellida feels a deep connection to the sea and longs for the freedom it represents. Her sense of unease is compounded by the arrival of a mysterious stranger, the Stranger, who claims to have a previous connection to Ellida. As the play unfolds, Ellida is torn between her sense of duty to her family and her desire for personal autonomy. The presence of the Stranger awakens conflicting emotions within her, leading to a series of dramatic confrontations and revelations.',
        46000,
        40,
        U&'https://books.google.com/books/content?id=PW3_EAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'BoD - Books on Demand',
        U&'9791041996094',
        2024,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Poetry"]'::jsonb,
        102,
        U&'B\00ECa m\1EC1m',
        55000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9791041996094'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 40, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9791041996094'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Atlas of AI',
        U&'Kate Crawford',
        U&'The hidden costs of artificial intelligence, from natural resources and labor to privacy and freedom What happens when artificial intelligence saturates political life and depletes the planet? How is AI shaping our understanding of ourselves and our societies? In this book Kate Crawford reveals how this planetary network is fueling a shift toward undemocratic governance and increased inequality. Drawing on more than a decade of research, award-winning science, and technology, Crawford reveals how AI is a technology of extraction: from the energy and minerals needed to build and sustain its infrastructure, to the exploited workers behind "automated" services, to the data AI collects from us. Rather than taking a narrow focus on code and algorithms, Crawford offers us a political and a material perspective on what it takes to make artificial intelligence and where it goes wrong. While technical systems present a veneer of objectivity, they are always systems of power. This is an urgent account of what is at stake as technology companies use artificial intelligence to reshape the world.',
        151000,
        49,
        U&'https://books.google.com/books/content?id=KfodEAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Yale University Press',
        U&'9780300209570',
        2021,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Computers"]'::jsonb,
        336,
        U&'B\00ECa m\1EC1m',
        181000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780300209570'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 49, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780300209570'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Cybernetics Or Control and Communication in the Animal and the Machine',
        U&'Norbert Wiener',
        U&'It appers impossible for anyone seriously interested in our civilization to ignore this book. It is a ''must'' book for those in every branch of science . . . in addition, economists, politicians, statesmen, and businessmen cannot afford to overlook cybernetics and its tremendous, even terrifying implications.',
        110000,
        41,
        U&'https://books.google.com/books/content?id=NnM-uISyywAC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'MIT Press',
        U&'9780262730099',
        1961,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Computers"]'::jsonb,
        244,
        U&'B\00ECa m\1EC1m',
        132000,
        5,
        1,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780262730099'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 41, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780262730099'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Audio Sampling',
        U&'Sam McGuire',
        U&'Bringing sampling to a new generation of audio engineers and composers Audio Sampling explains how to record and create sampled instruments in a software setting. There are many things that go into creating a sampled instrument and many things that can go wrong, this book is a step by step guide through the process, from introducing sampling, where it begins to recording editing and using samples, providing much sought after detailed information on the actual process of sampling, creating sampled instruments as well as the different ways they can be used. The software used is the NN-XT a sampler that is a part of the Reason studio software and ProTools LE, however the material discussed is applicable and can be used with any sampler. The companion website has exclusive material including a comprehensive comparison of the different hardware software available, as well as audio examples and video clips from each stage of the process',
        104000,
        13,
        U&'https://books.google.com/books/content?id=L81Gkb0z3UUC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Taylor & Francis',
        U&'9780240520735',
        2008,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Computers"]'::jsonb,
        232,
        U&'B\00ECa m\1EC1m',
        125000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780240520735'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 13, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780240520735'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'Microsoft Secrets',
        U&'Michael A. Cusumano',
        U&'Based on highly confidential interviews with personnel, internal memos, and top-secret company documents, this compelling portrait reveals the philosophy, style, and competitive strategies that have taken Microsoft to the heights of the high-tech industry.',
        239000,
        34,
        U&'https://books.google.com/books/content?id=GixEgGs5qXcC&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Simon and Schuster',
        U&'9780684855318',
        1998,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Business \005Cu0026 Economics"]'::jsonb,
        532,
        U&'B\00ECa m\1EC1m',
        287000,
        5,
        1,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9780684855318'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 34, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9780684855318'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'The Tower of Hanoi \2013 Myths and Maths',
        U&'Andreas M. Hinz',
        U&'This is the first comprehensive monograph on the mathematical theory of the solitaire game \201CThe Tower of Hanoi\201D which was invented in the 19th century by the French number theorist \00C9douard Lucas. The book comprises a survey of the historical development from the game\2019s predecessors up to recent research in mathematics and applications in computer science and psychology. Apart from long-standing myths it contains a thorough, largely self-contained presentation of the essential mathematical facts with complete proofs, including also unpublished material. The main objects of research today are the so-called Hanoi graphs and the related Sierpi\0144ski graphs. Acknowledging the great popularity of the topic in computer science, algorithms and their correctness proofs form an essential part of the book. In view of the most important practical applications of the Tower of Hanoi and its variants, namely in physics, network theory, and cognitive (neuro)psychology, other related structures and puzzles like, e.g., the \201CTower of London\201D, are addressed. Numerous captivating integer sequences arise along the way, but also many open questions impose themselves. Central among these is the famed Frame-Stewart conjecture. Despite many attempts to decide it and large-scale numerical experiments supporting its truth, it remains unsettled after more than 70 years and thus demonstrates the timeliness of the topic. Enriched with elaborate illustrations, connections to other puzzles and challenges for the reader in the form of (solved) exercises as well as problems for further exploration, this book is enjoyable reading for students, educators, game enthusiasts and researchers alike.',
        1293522,
        74,
        U&'https://books.google.com/books/content?id=FbJDAAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'Springer Science & Business Media',
        U&'9783034802376',
        2013,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Mathematics"]'::jsonb,
        340,
        U&'B\00ECa m\1EC1m',
        1847888,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9783034802376'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 74, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9783034802376'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'TinyML',
        U&'Pete Warden',
        U&'Deep learning networks are getting smaller. Much smaller. The Google Assistant team can detect words with a model just 14 kilobytes in size\2014small enough to run on a microcontroller. With this practical book you\2019ll enter the field of TinyML, where deep learning and embedded systems combine to make astounding things possible with tiny devices. Pete Warden and Daniel Situnayake explain how you can train models small enough to fit into any environment. Ideal for software and hardware developers who want to build embedded systems using machine learning, this guide walks you through creating a series of TinyML projects, step-by-step. No machine learning or microcontroller experience is necessary. Build a speech recognizer, a camera that detects people, and a magic wand that responds to gestures Work with Arduino and ultra-low-power microcontrollers Learn the essentials of ML and how to train your own models Train models to understand audio, image, and accelerometer data Explore TensorFlow Lite for Microcontrollers, Google\2019s toolkit for TinyML Debug applications and provide safeguards for privacy and security Optimize latency, energy usage, and model and binary size',
        227000,
        58,
        U&'https://books.google.com/books/content?id=tn3EDwAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'O''Reilly Media',
        U&'9781492052012',
        2019,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Computers"]'::jsonb,
        504,
        U&'B\00ECa m\1EC1m',
        272000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781492052012'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 58, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781492052012'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = U&'C\00F4ng ngh\1EC7 th\00F4ng tin'
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        U&'UX Strategy',
        U&'Jaime Levy',
        U&'User experience (UX) strategy requires a careful blend of business strategy and UX design, and this hands-on guide offers an easy-to-apply framework for executing it. It is packed with product strategy tools and tactics to help you and your team craft innovative solutions that people want. This second edition includes new real-world examples, updated techniques and a chapter on conducting qualitative online user research. Whether you''re a UX/UI designer, product manager/owner, entrepreneur, or member of a corporate innovation team, this book teaches simple to advanced methods that you can use in your work right away. You''ll also gain perspective on the subject matter through historical context and case studies. Define value propositions and validate target users through provisional personas and customer discovery techniques Conduct methodical competitive research on direct and indirect competitors and create an analysis brief to decisively guide stakeholders Use storyboarding and rapid prototyping for designing experiments that focus on the value innovation and business model of your product Learn how to conduct user research online to get valuable insights quickly on any budget Test business ideas and validate marketing channels by running online advertising and landing page campaigns.',
        136000,
        13,
        U&'https://books.google.com/books/content?id=-BUjEAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api',
        NULL,
        TRUE,
        U&'"O''Reilly Media, Inc."',
        U&'9781492052401',
        2021,
        U&'en',
        U&'["C\00F4ng ngh\1EC7 th\00F4ng tin","Business \005Cu0026 Economics"]'::jsonb,
        302,
        U&'B\00ECa m\1EC1m',
        163000,
        0,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = U&'9781492052401'
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, 13, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = U&'9781492052401'
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
COMMIT;
