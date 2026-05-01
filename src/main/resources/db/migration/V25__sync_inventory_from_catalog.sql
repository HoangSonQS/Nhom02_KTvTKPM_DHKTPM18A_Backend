-- V25__sync_inventory_from_catalog.sql
-- Sync existing book quantities from cat_book (deprecated_quantity) to inv_stock
-- This ensures that books created before the inventory module was fully integrated have stock records.

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, deprecated_quantity, 0, NOW(), NOW()
FROM cat_book
ON CONFLICT (book_id) DO UPDATE 
SET quantity = EXCLUDED.quantity,
    updated_at = NOW();

-- Also ensure history is updated or at least consistent if needed, 
-- but for now, the primary goal is to have stock records.
