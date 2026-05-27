-- Fix mojibake in Vietnamese administrative unit names.
-- V33/V40 data was stored as UTF-8 bytes decoded as LATIN1.

CREATE OR REPLACE FUNCTION acc_fix_mojibake(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    IF value IS NULL THEN
        RETURN NULL;
    END IF;

    BEGIN
        RETURN convert_from(convert_to(value, 'LATIN1'), 'UTF8');
    EXCEPTION WHEN others THEN
        RETURN value;
    END;
END;
$$;

UPDATE acc_provinces
SET name = acc_fix_mojibake(name),
    full_name = acc_fix_mojibake(full_name);

UPDATE acc_wards
SET name = acc_fix_mojibake(name),
    full_name = acc_fix_mojibake(full_name);

DROP FUNCTION acc_fix_mojibake(TEXT);
