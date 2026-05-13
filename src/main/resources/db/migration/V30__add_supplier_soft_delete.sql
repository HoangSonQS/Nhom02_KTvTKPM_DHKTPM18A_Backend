ALTER TABLE log_supplier
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_log_supplier_is_deleted
    ON log_supplier(is_deleted);
