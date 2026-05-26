ALTER TABLE prm_flash_sale
    DROP CONSTRAINT IF EXISTS chk_prm_flash_sale_quantity;

ALTER TABLE prm_flash_sale
    ADD CONSTRAINT chk_prm_flash_sale_quantity CHECK (sale_quantity >= 0);
