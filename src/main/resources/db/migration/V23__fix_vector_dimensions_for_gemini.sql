-- V23__fix_vector_dimensions_for_gemini.sql
-- Update ai_book_vectors table to use 768 dimensions for Gemini text-embedding-004

-- 1. Drop existing HNSW index
DROP INDEX IF EXISTS ai_book_vectors_embedding_idx;

-- 2. Alter the embedding column to change dimensions to 768
ALTER TABLE ai_book_vectors 
ALTER COLUMN embedding TYPE VECTOR(768);

-- 3. Recreate the HNSW index
CREATE INDEX ai_book_vectors_embedding_idx ON ai_book_vectors USING hnsw (embedding vector_cosine_ops);

-- 4. Truncate the table to force re-sync
TRUNCATE TABLE ai_book_vectors;
