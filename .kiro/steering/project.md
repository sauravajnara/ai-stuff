# RAG Search Engine - Project Steering

## Stack
- Java 17, Spring Boot 3.3.4, Gradle
- LangChain4j 1.0.0-beta2
- Embeddings: `all-MiniLM-L6-v2` (local ONNX, 384 dimensions, no API cost)
- Chat LLM: Groq `llama-3.3-70b-versatile` via OpenAI-compatible API
- Vector store: pgvector on PostgreSQL (port 5433, database `ragdb`)
- Document parsing: Apache PDFBox (PDF), Apache POI (DOCX), plain text
- Observability: Spring Actuator, Micrometer, Prometheus, OpenTelemetry (OTLP), JSON logging (logstash-logback-encoder)
- API Docs: springdoc-openapi (Swagger UI at `/swagger-ui/index.html`)
- Frontend: Angular 21 app at `/Users/saurabhkumar/Documents/rag-engine-ui` (separate from backend)

## Project Structure
```
com.rag.engine.ragsearchengine
├── controller/
│   ├── RagController.java          # /api/rag/* endpoints
│   ├── DocumentController.java     # /api/documents/* endpoints
│   └── GlobalExceptionHandler.java # @RestControllerAdvice — JSON error responses
├── service/
│   ├── DocumentIngestionService.java  # parse → split → embed → pgvector
│   ├── RagQueryService.java           # embed question → search → LLM answer
│   ├── RagEvaluationService.java      # faithfulness + relevancy scoring via LLM
│   └── QueryLogService.java           # persist query logs, compute stats
├── config/
│   ├── RagConfig.java         # EmbeddingModel (AllMiniLM) + EmbeddingStore (PgVector, dim=384)
│   ├── ChatModelConfig.java   # ChatLanguageModel (Groq via OpenAI-compatible client)
│   └── SwaggerConfig.java     # OpenAPI bean (title, description, version)
├── model/
│   ├── IngestedDocument.java  # JPA entity — ingested_documents table
│   ├── QueryLog.java          # JPA entity — query_logs table
│   ├── RagResponse.java       # answer + retrievedChunks + faithfulness + answerRelevancy
│   └── RagStats.java          # avgFaithfulness, avgAnswerRelevancy, avgLatencyMs, avgChunkScore, totalQueries
└── repository/
    ├── IngestedDocumentRepository.java
    └── QueryLogRepository.java        # custom @Query for avg metrics
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/documents/upload` | Upload and ingest PDF, DOCX, DOC, or TXT (max 50MB) |
| GET  | `/api/documents` | List all ingested documents |
| DELETE | `/api/documents/{id}` | Delete document metadata (note: does NOT remove pgvector embeddings) |
| GET  | `/api/rag/query?question=...` | Simple query — returns plain string answer |
| GET  | `/api/rag/query/evaluate?question=...` | Query with RAGAS eval — returns `RagResponse` (answer + scores) |
| GET  | `/api/rag/stats` | Aggregate stats from query_logs |
| GET  | `/actuator/health` | Health check |
| GET  | `/actuator/metrics` | Micrometer metrics |
| GET  | `/actuator/prometheus` | Prometheus scrape endpoint |
| GET  | `/swagger-ui/index.html` | Swagger UI |

## Ingestion Pipeline
Document → parse (PDFBox / POI / Text) → split (500 chars, 50 overlap) → embed (384d AllMiniLM) → store in pgvector → save `IngestedDocument` record

## Query Pipeline
Question → embed (384d) → pgvector similarity search (top 5, min score 0.6) → build prompt → Groq LLM → answer

## Evaluated Query Pipeline (`/query/evaluate`)
Same as above + faithfulness score + relevancy score (both via LLM) + log to `query_logs` table → return `RagResponse`

## Data Models

### RagResponse (JSON)
```json
{
  "answer": "...",
  "retrievedChunks": ["...", "..."],
  "faithfulness": 0.92,
  "answerRelevancy": 0.87
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
| `OTEL_EXPORTER_OTLP_ENDPOINT` | no | OTLP collector URL (default: `http://localhost:4318/v1/traces`) |

## Key Config
- pgvector: `localhost:5433`, database `ragdb`
- Embedding dimension: 384 — never change without dropping and recreating `document_embeddings` table
- File upload limit: 50MB
- Allowed file types: `.pdf`, `.docx`, `.doc`, `.txt`
- Question max length: 500 characters
- `spring.jpa.hibernate.ddl-auto=update` — JPA manages `ingested_documents` and `query_logs` tables
- pgvector extension bootstrapped via `schema.sql`: `CREATE EXTENSION IF NOT EXISTS vector;`

## Conventions
- Constructor injection only — no `@Autowired`
- All new services in `service/`, all new endpoints in `controller/` with `@RestController`
- HTTP client: `langchain4j-http-client-spring-restclient` — do NOT add `langchain4j-open-ai-spring-boot-starter` (causes HTTP client conflict)
- Error responses are always JSON `{ "error": "..." }` via `GlobalExceptionHandler`
- DELETE `/api/documents/{id}` removes metadata only — pgvector embeddings are NOT deleted (known limitation)

## Frontend (Angular)
- Location: `/Users/saurabhkumar/Documents/rag-engine-ui`
- Angular 21, standalone components, plain CSS
- 3 views: Query (chat), Documents (upload + list), Stats
- API proxy: `/api/*` → `http://localhost:8080` via `proxy.conf.json`
- Run: `ng serve` from the `rag-engine-ui` directory
