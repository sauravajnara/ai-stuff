CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS semantic_query_cache (
    id              BIGSERIAL PRIMARY KEY,
    question        TEXT NOT NULL,
    answer          TEXT NOT NULL,
    question_embedding vector(384),
    faithfulness    DOUBLE PRECISION,
    answer_relevancy DOUBLE PRECISION,
    cached_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_embedding
    ON semantic_query_cache USING ivfflat (question_embedding vector_cosine_ops);
