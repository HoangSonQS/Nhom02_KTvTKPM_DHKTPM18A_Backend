-- Decode remaining mixed CP1252/control mojibake in Vietnamese administrative unit names.

CREATE OR REPLACE FUNCTION acc_prepare_cp1252_controls(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    result TEXT := value;
BEGIN
    IF result IS NULL THEN
        RETURN NULL;
    END IF;

    result := replace(result, chr(8364), chr(128));
    result := replace(result, chr(8218), chr(130));
    result := replace(result, chr(402), chr(131));
    result := replace(result, chr(8222), chr(132));
    result := replace(result, chr(8230), chr(133));
    result := replace(result, chr(8224), chr(134));
    result := replace(result, chr(8225), chr(135));
    result := replace(result, chr(710), chr(136));
    result := replace(result, chr(8240), chr(137));
    result := replace(result, chr(352), chr(138));
    result := replace(result, chr(8249), chr(139));
    result := replace(result, chr(338), chr(140));
    result := replace(result, chr(381), chr(142));
    result := replace(result, chr(8216), chr(145));
    result := replace(result, chr(8217), chr(146));
    result := replace(result, chr(8220), chr(147));
    result := replace(result, chr(8221), chr(148));
    result := replace(result, chr(8226), chr(149));
    result := replace(result, chr(8211), chr(150));
    result := replace(result, chr(8212), chr(151));
    result := replace(result, chr(732), chr(152));
    result := replace(result, chr(8482), chr(153));
    result := replace(result, chr(353), chr(154));
    result := replace(result, chr(8250), chr(155));
    result := replace(result, chr(339), chr(156));
    result := replace(result, chr(382), chr(158));
    result := replace(result, chr(376), chr(159));

    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION acc_decode_mojibake_layer(value TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    RETURN convert_from(convert_to(acc_prepare_cp1252_controls(value), 'LATIN1'), 'UTF8');
EXCEPTION WHEN others THEN
    RETURN value;
END;
$$;

CREATE OR REPLACE FUNCTION acc_normalize_cp1252_control_text(value TEXT)
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
        next_value := acc_decode_mojibake_layer(current_value);

        IF next_value = current_value THEN
            RETURN current_value;
        END IF;

        current_value := next_value;
    END LOOP;

    RETURN current_value;
END;
$$;

UPDATE acc_provinces
SET name = acc_normalize_cp1252_control_text(name),
    full_name = acc_normalize_cp1252_control_text(full_name);

UPDATE acc_wards
SET name = acc_normalize_cp1252_control_text(name),
    full_name = acc_normalize_cp1252_control_text(full_name);

DROP FUNCTION acc_normalize_cp1252_control_text(TEXT);
DROP FUNCTION acc_decode_mojibake_layer(TEXT);
DROP FUNCTION acc_prepare_cp1252_controls(TEXT);
