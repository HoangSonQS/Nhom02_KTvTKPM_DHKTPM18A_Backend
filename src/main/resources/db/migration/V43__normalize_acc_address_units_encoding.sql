-- Normalize Vietnamese administrative unit names after mixed mojibake repairs.
-- Applies WIN1252-to-UTF8 decoding repeatedly until the text is stable or already valid.

CREATE OR REPLACE FUNCTION acc_normalize_vietnamese_text(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    current_value TEXT := value;
    next_value TEXT;
    attempt INTEGER;
BEGIN
    IF current_value IS NULL THEN
        RETURN NULL;
    END IF;

    FOR attempt IN 1..3 LOOP
        BEGIN
            next_value := convert_from(convert_to(current_value, 'WIN1252'), 'UTF8');
        EXCEPTION WHEN others THEN
            RETURN current_value;
        END;

        IF next_value = current_value THEN
            RETURN current_value;
        END IF;

        current_value := next_value;
    END LOOP;

    RETURN current_value;
END;
$$;

UPDATE acc_provinces
SET name = acc_normalize_vietnamese_text(name),
    full_name = acc_normalize_vietnamese_text(full_name);

UPDATE acc_wards
SET name = acc_normalize_vietnamese_text(name),
    full_name = acc_normalize_vietnamese_text(full_name);

DROP FUNCTION acc_normalize_vietnamese_text(TEXT);
