-- V16__add_published_at_to_outbox.sql
-- Add published_at column to ret_outbox_event table for Hibernate schema validation

ALTER TABLE ret_outbox_event ADD COLUMN published_at TIMESTAMP;
