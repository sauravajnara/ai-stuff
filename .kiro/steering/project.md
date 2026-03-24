# RAG Search Engine - Project Steering

## Stack
- Java 17, Spring Boot 3.3.4, Gradle
- LangChain4j 1.0.0-beta2
- Embeddings: `all-MiniLM-L6-v2` (local ONNX, 384 dimensions, no API cost)
- Chat LLM: Groq `llama-3.3-70b-versatile` via OpenAI-compatible API
- Vector store: pgvector on PostgreSQL (port 5433, database `ragdb`)
- Document parsing: Apache PDFBox (PDF), Apache POI (DOCX), plain text

## Project Structure
```
com.rag.engine.ragsearchengine
├── controller/       # REST endpoints
├── service/          # Business logic
├── config/           # Spring beans (RagConfig, ChatModelConfig)
├── model/            # Domain models
└── repository/       # JPA repositories
```

## Key Config Classes
- `RagConfig` — defines `EmbeddingModel` (local AllMiniLM) and `EmbeddingStore` (PgVector, dimension=384)
- `ChatModelConfig` — defines `ChatLanguageModel` (Groq via OpenAI-compatible client)

## API Endpoints
- `POST /api/documents/upload` — upload and ingest a PDF, DOCX, or TXT file
- `GET  /api/rag/query?question=...` — query against ingested documents

## Ingestion Pipeline
Document → split (500 chars, 50 overlap) → embed (384d) → store in pgvector

## Query Pipeline
Question → embed → pgvector similarity search (top 5, min score 0.6) → Groq LLM answers from context

## Conventions
- All new services go in `service/`, injected via constructor injection (no @Autowired)
- All new endpoints go in `controller/` with `@RestController`
- Embedding dimension is 384 — do not change without dropping and recreating the `document_embeddings` table
- pgvector extension must exist: `CREATE EXTENSION IF NOT EXISTS vector;` (handled by schema.sql)
- Environment variables: `GROQ_API_KEY`, `DB_USER`, `DB_PASSWORD`
