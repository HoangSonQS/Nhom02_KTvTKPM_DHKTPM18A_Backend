-- V22__update_ai_vector_dimensions.sql
-- Update ai_book_vectors table to use 1024 dimensions for Cohere v3 models

-- 1. Drop existing HNSW index (as it depends on the column dimensions)
DROP INDEX IF EXISTS ai_book_vectors_embedding_idx;

-- 2. Alter the embedding column to change dimensions from 1536 to 1024
ALTER TABLE ai_book_vectors 
ALTER COLUMN embedding TYPE VECTOR(1024);

-- 3. Recreate the HNSW index for distance calculation
CREATE INDEX ai_book_vectors_embedding_idx ON ai_book_vectors USING hnsw (embedding vector_cosine_ops);

-- 4. Truncate the table because old vectors with different dimensions are no longer valid 
-- and we changed the provider/model. EmbeddingSyncService will rebuild them.
TRUNCATE TABLE ai_book_vectors;
