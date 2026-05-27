-- Normalize mixed LATIN1/WIN1252 mojibake layers in Vietnamese administrative unit names.

CREATE OR REPLACE FUNCTION acc_decode_latin1(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    RETURN convert_from(convert_to(value, 'LATIN1'), 'UTF8');
EXCEPTION WHEN others THEN
    RETURN value;
END;
$$;

CREATE OR REPLACE FUNCTION acc_decode_win1252(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    RETURN convert_from(convert_to(value, 'WIN1252'), 'UTF8');
EXCEPTION WHEN others THEN
    RETURN value;
END;
$$;

CREATE OR REPLACE FUNCTION acc_normalize_mixed_vietnamese_text(value TEXT)
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

    FOR attempt IN 1..6 LOOP
        next_value := acc_decode_latin1(current_value);

        IF next_value = current_value THEN
            next_value := acc_decode_win1252(current_value);
        END IF;

        IF next_value = current_value THEN
            RETURN current_value;
        END IF;

        current_value := next_value;
    END LOOP;

    RETURN current_value;
END;
$$;

UPDATE acc_provinces
SET name = acc_normalize_mixed_vietnamese_text(name),
    full_name = acc_normalize_mixed_vietnamese_text(full_name);

UPDATE acc_wards
SET name = acc_normalize_mixed_vietnamese_text(name),
    full_name = acc_normalize_mixed_vietnamese_text(full_name);

DROP FUNCTION acc_normalize_mixed_vietnamese_text(TEXT);
DROP FUNCTION acc_decode_win1252(TEXT);
DROP FUNCTION acc_decode_latin1(TEXT);
