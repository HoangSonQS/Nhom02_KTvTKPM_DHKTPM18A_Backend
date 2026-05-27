ALTER TABLE ret_return_requests
    ADD COLUMN evidence_image_url VARCHAR(1000),
    ADD COLUMN evidence_image_public_id VARCHAR(255);
