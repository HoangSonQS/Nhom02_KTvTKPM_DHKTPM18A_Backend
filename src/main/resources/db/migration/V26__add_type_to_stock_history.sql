-- V26__add_type_to_stock_history.sql
ALTER TABLE inv_stock_history ADD COLUMN type VARCHAR(50);
UPDATE inv_stock_history SET type = 'DECREASE' WHERE type IS NULL;
ALTER TABLE inv_stock_history ALTER COLUMN type SET NOT NULL;
