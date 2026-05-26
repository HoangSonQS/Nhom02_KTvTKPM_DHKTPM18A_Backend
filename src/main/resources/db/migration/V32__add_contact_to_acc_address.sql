-- Add recipient contact fields to account addresses.
ALTER TABLE acc_address
    ADD COLUMN recipient_name VARCHAR(120),
    ADD COLUMN phone_number VARCHAR(20);
