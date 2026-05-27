-- Fix Windows-1252 mojibake in Vietnamese administrative unit names.
-- Some rows contain CP1252 characters such as smart quotes from decoded UTF-8 bytes.

CREATE OR REPLACE FUNCTION acc_fix_win1252_mojibake(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    IF value IS NULL THEN
        RETURN NULL;
    END IF;

    BEGIN
        RETURN convert_from(convert_to(value, 'WIN1252'), 'UTF8');
    EXCEPTION WHEN others THEN
        RETURN value;
    END;
END;
$$;

UPDATE acc_provinces
SET name = acc_fix_win1252_mojibake(name),
    full_name = acc_fix_win1252_mojibake(full_name);

UPDATE acc_wards
SET name = acc_fix_win1252_mojibake(name),
    full_name = acc_fix_win1252_mojibake(full_name);

DROP FUNCTION acc_fix_win1252_mojibake(TEXT);
