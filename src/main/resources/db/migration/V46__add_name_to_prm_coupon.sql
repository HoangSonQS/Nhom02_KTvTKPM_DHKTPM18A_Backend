ALTER TABLE prm_coupon
    ADD COLUMN name VARCHAR(150);

UPDATE prm_coupon
SET name = LEFT(COALESCE(NULLIF(TRIM(description), ''), code), 150)
WHERE name IS NULL;

ALTER TABLE prm_coupon
    ALTER COLUMN name SET NOT NULL;
