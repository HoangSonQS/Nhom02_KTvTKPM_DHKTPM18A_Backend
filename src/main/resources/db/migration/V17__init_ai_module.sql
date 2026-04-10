-- V17__init_ai_module.sql
-- Create extension and tables for AI module (Vector Search & Chat)

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Table for storing Book vectors (Spring AI compatible)
CREATE TABLE ai_book_vectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);

-- Index for distance calculation (HNSW)
CREATE INDEX ON ai_book_vectors USING hnsw (embedding vector_cosine_ops);

-- 3. Tables for Chat History
CREATE TABLE ai_chat_sessions (
    id VARCHAR(50) PRIMARY KEY,
    customer_id BIGINT, -- Can be null for guests
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_session FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_session ON ai_chat_messages(session_id);
