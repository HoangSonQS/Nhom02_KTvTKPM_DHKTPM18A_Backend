ALTER TABLE pay_payment ADD COLUMN refund_amount DECIMAL(19, 2);
ALTER TABLE pay_payment ADD COLUMN return_request_id VARCHAR(36);
