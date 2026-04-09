ALTER TABLE adm_order_report ADD COLUMN refund_amount DECIMAL(19, 2);
ALTER TABLE adm_order_report ADD COLUMN refunded_at TIMESTAMP;
