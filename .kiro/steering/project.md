# RAG Search Engine - Project Steering

## Stack
- Java 17, Spring Boot 3.3.4, Gradle
- LangChain4j 1.0.0-beta2
- Embeddings: `all-MiniLM-L6-v2` (local ONNX, 384 dimensions, no API cost)
- Chat LLM: Groq `llama-3.3-70b-versatile` via OpenAI-compatible API
- Vector store: pgvector on PostgreSQL (port 5433, database `ragdb`)
- Document parsing: Apache PDFBox (PDF), Apache POI (DOCX), plain text
- Caching: Redis (exact match, TTL 60min) + pgvector semantic cache (cosine ≥ 0.95, TTL 7 days), master flag `rag.cache.enabled`
- Resilience: Resilience4j circuit breaker + retry on LLM calls
- Observability: Spring Actuator, Micrometer, Prometheus, OpenTelemetry (OTLP), JSON logging (logstash-logback-encoder)
- API Docs: springdoc-openapi (Swagger UI at `/swagger-ui/index.html`)
- Frontend: Angular 21 app at `/Users/saurabhkumar/Documents/rag-engine-ui` (separate from backend)

## Project Structure
```
com.rag.engine.ragsearchengine
├── controller/
│   ├── RagController.java             # /api/rag/* endpoints
│   ├── DocumentController.java        # /api/documents/* endpoints
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice — JSON error responses
├── service/
│   ├── DocumentIngestionService.java  # parse → split → embed → pgvector
│   ├── RagQueryService.java           # layered cache → full RAG pipeline
│   ├── RagEvaluationService.java      # faithfulness + relevancy scoring via LLM
│   ├── QueryLogService.java           # persist query logs, compute stats
│   └── SemanticCacheService.java      # Redis exact + pgvector semantic cache
├── config/
│   ├── RagConfig.java         # EmbeddingModel (AllMiniLM) + EmbeddingStore (PgVector, dim=384)
│   ├── ChatModelConfig.java   # ChatLanguageModel (Groq via OpenAI-compatible client)
│   ├── CacheConfig.java       # RedisCacheManager with per-cache TTLs
│   └── SwaggerConfig.java     # OpenAPI bean
├── model/
│   ├── IngestedDocument.java      # JPA entity — ingested_documents table
│   ├── QueryLog.java              # JPA entity — query_logs table
│   ├── SemanticCacheEntry.java    # JPA entity — semantic_query_cache table
│   ├── RagResponse.java           # answer + retrievedChunks + faithfulness + answerRelevancy + cacheHit
│   └── RagStats.java              # avgFaithfulness, avgAnswerRelevancy, avgLatencyMs, avgChunkScore, totalQueries
└── repository/
    ├── IngestedDocumentRepository.java
    ├── QueryLogRepository.java          # custom @Query for avg metrics
    └── SemanticCacheRepository.java     # native pgvector cosine similarity + stale eviction
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/documents/upload` | Upload and ingest PDF, DOCX, DOC, or TXT (max 50MB) |
| GET  | `/api/documents` | List all ingested documents |
| DELETE | `/api/documents/{id}` | Delete document metadata (note: does NOT remove pgvector embeddings) |
| GET  | `/api/rag/query?question=...` | Simple query — returns plain string answer |
| GET  | `/api/rag/query/evaluate?question=...` | Query with RAGAS eval — returns `RagResponse` (answer + scores + cacheHit) |
| GET  | `/api/rag/stats` | Aggregate stats from query_logs |
| GET  | `/actuator/health` | Health check (includes circuit breaker state) |
| GET  | `/actuator/metrics` | Micrometer metrics |
| GET  | `/actuator/prometheus` | Prometheus scrape endpoint |
| GET  | `/swagger-ui/index.html` | Swagger UI |

## Ingestion Pipeline
Document → parse (PDFBox / POI / Text) → split (500 chars, 50 overlap) → embed (384d AllMiniLM) → store in pgvector → save `IngestedDocument` record

## Query Pipeline (layered cache)
```
Incoming question
      ↓
[1] Redis exact match     (~1ms,  TTL 60min)       ← skipped if rag.cache.enabled=false
      ↓ miss
[2] pgvector semantic     (~20ms, cosine ≥ 0.95)   ← skipped if rag.cache.enabled=false
      ↓ miss
[3] Full RAG pipeline     (~2-5s, embed → pgvector search → Groq LLM)
      ↓
Store result in Redis + semantic_query_cache        ← skipped if rag.cache.enabled=false
```

## Evaluated Query Pipeline (`/query/evaluate`)
Same layered cache flow + on full pipeline miss: faithfulness + relevancy scoring via LLM + log to `query_logs` → return `RagResponse`

## Data Models

### RagResponse (JSON)
```json
{
  "answer": "...",
  "retrievedChunks": ["...", "..."],
  "faithfulness": 0.92,
  "answerRelevancy": 0.87,
  "cacheHit": false
}
```

### RagStats (JSON)
```json
{
  "avgFaithfulness": 0.88,
  "avgAnswerRelevancy": 0.85,
  "avgLatencyMs": 1240.5,
  "avgChunkScore": 0.74,
  "totalQueries": 42
}
```

### IngestedDocument (JSON)
```json
{
  "id": 1,
  "filename": "manual.pdf",
  "chunkCount": 34,
  "status": "COMPLETED",
  "uploadedAt": "2026-03-24T10:00:00"
}
```

## Caching
- Master switch: `rag.cache.enabled=true` — set to `false` to bypass both layers entirely (useful for debugging or testing fresh results)
- `SemanticCacheService` checks `cacheEnabled` flag in `getExact()`, `getSemantic()`, and `put()` — no cache I/O when disabled
- Redis cache names and TTLs (`CacheConfig`): `rag-exact` 60min, `rag-stats` 30s, `doc-list` 5min
- Semantic cache table: `semantic_query_cache` (pgvector `vector(384)`, ivfflat cosine index)
- Stale semantic entries evicted nightly at 2am via `@Scheduled` in `SemanticCacheService`
- Both layers fail silently — Redis/pgvector unavailability falls through to full pipeline

## Resilience
- Circuit breaker: `llmCircuitBreaker` on `query()` and `queryWithEval()` — opens after 50% failure rate over 10 calls, waits 30s
- Retry: `llmRetry` — 3 attempts, 2s initial wait, exponential backoff (×2)
- Fallback: returns user-friendly unavailability message when circuit is open
- Circuit breaker state exposed via `/actuator/health`

## Observability
- Actuator endpoints: `health`, `info`, `metrics`, `prometheus`
- Distributed tracing: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
- OTLP endpoint: `${OTEL_EXPORTER_OTLP_ENDPOINT}` (default: `http://localhost:4318/v1/traces`)
- Structured JSON logging on `prod` profile via `logback-spring.xml` (logstash-logback-encoder)
- Human-readable logging with traceId on `dev` profile
- Tracing sampling: 1.0 (100%)

## Environment Variables
| Variable | Required | Description |
|----------|----------|-------------|
| `GROQ_API_KEY` | yes | Groq API key — never hardcode |
| `DB_USER` | no | Postgres username (default: `postgres`) |
| `DB_PASSWORD` | no | Postgres password (default: `password`) |
| `REDIS_HOST` | no | Redis host (default: `localhost`) |
| `REDIS_PORT` | no | Redis port (default: `6379`) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | no | OTLP collector URL (default: `http://localhost:4318/v1/traces`) |

## Key Config
- pgvector: `localhost:5433`, database `ragdb`
- Embedding dimension: 384 — never change without dropping and recreating `document_embeddings` AND `semantic_query_cache` tables
- File upload limit: 50MB
- Allowed file types: `.pdf`, `.docx`, `.doc`, `.txt`
- Question max length: 500 characters
- `spring.jpa.hibernate.ddl-auto=update` — JPA manages `ingested_documents`, `query_logs`, `semantic_query_cache` tables
- `schema.sql` bootstraps: pgvector extension, `semantic_query_cache` table + ivfflat index
- `@EnableScheduling` on `RagSearchEngineApplication` — required for nightly cache eviction
- Cache on/off: `rag.cache.enabled=true` (default) — controls both Redis and semantic cache layers

## Conventions
- Constructor injection only — no `@Autowired`
- All new services in `service/`, all new endpoints in `controller/` with `@RestController`
- HTTP client: `langchain4j-http-client-spring-restclient` — do NOT add `langchain4j-open-ai-spring-boot-starter` (causes HTTP client conflict)
- Error responses are always JSON `{ "error": "..." }` via `GlobalExceptionHandler`
- Cache failures must never break the query flow — always wrap cache calls in try/catch
- DELETE `/api/documents/{id}` removes metadata only — pgvector embeddings are NOT deleted (known limitation)

## Local Dev Prerequisites
```bash
# PostgreSQL with pgvector
docker run -d -p 5433:5432 -e POSTGRES_DB=ragdb -e POSTGRES_PASSWORD=password pgvector/pgvector:pg16

# Redis
docker run -d -p 6379:6379 redis:alpine
```

## Frontend (Angular)
- Location: `/Users/saurabhkumar/Documents/rag-engine-ui`
- Angular 21, standalone components, plain CSS
- 3 views: Query (chat), Documents (upload + list), Stats
- API proxy: `/api/*` → `http://localhost:8080` via `proxy.conf.json`
- Run: `ng serve` from the `rag-engine-ui` directory
