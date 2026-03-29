package com.rag.engine.ragsearchengine.service;

import com.rag.engine.ragsearchengine.model.RagResponse;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
            "Answer the question based only on the context below.\n\n" +
            "Context:\n{{context}}\n\n" +
            "Question: {{question}}"
    );

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    private final RagEvaluationService evaluationService;
    private final QueryLogService queryLogService;
    private final SemanticCacheService semanticCacheService;

    public RagQueryService(EmbeddingStore<TextSegment> embeddingStore,
                           EmbeddingModel embeddingModel,
                           ChatLanguageModel chatModel,
                           RagEvaluationService evaluationService,
                           QueryLogService queryLogService,
                           SemanticCacheService semanticCacheService) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.evaluationService = evaluationService;
        this.queryLogService = queryLogService;
        this.semanticCacheService = semanticCacheService;
    }

    @CircuitBreaker(name = "llmCircuitBreaker", fallbackMethod = "queryFallback")
    @Retry(name = "llmRetry")
    public String query(String question) {
        var queryEmbedding = embeddingModel.embed(question).content();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(5)
                        .minScore(0.6)
                        .build())
                .matches();

        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));

        Prompt prompt = PROMPT_TEMPLATE.apply(Map.of(
                "context", context,
                "question", question
        ));

        return chatModel.chat(prompt.toUserMessage()).aiMessage().text();
    }

    @CircuitBreaker(name = "llmCircuitBreaker", fallbackMethod = "queryWithEvalFallback")
    @Retry(name = "llmRetry")
    public RagResponse queryWithEval(String question) {
        // Layer 1: exact Redis cache
        Optional<RagResponse> exact = semanticCacheService.getExact(question);
        if (exact.isPresent()) {
            log.info("Returning exact cache hit for: {}", question);
            return exact.get();
        }

        // Layer 2: semantic pgvector cache
        Optional<RagResponse> semantic = semanticCacheService.getSemantic(question);
        if (semantic.isPresent()) {
            log.info("Returning semantic cache hit for: {}", question);
            // Also populate Redis for next exact hit
            semanticCacheService.put(question, semantic.get());
            return semantic.get();
        }

        // Layer 3: full RAG pipeline
        log.info("Cache miss — running full RAG pipeline for: {}", question);
        long start = System.currentTimeMillis();

        var queryEmbedding = embeddingModel.embed(question).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(5)
                        .minScore(0.6)
                        .build())
                .matches();

        List<String> chunks = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.toList());

        double avgChunkScore = matches.stream()
                .mapToDouble(EmbeddingMatch::score)
                .average().orElse(0.0);

        String context = String.join("\n\n", chunks);
        Prompt prompt = PROMPT_TEMPLATE.apply(Map.of("context", context, "question", question));
        String answer = chatModel.chat(prompt.toUserMessage()).aiMessage().text();
        long latencyMs = System.currentTimeMillis() - start;

        double faithfulness = evaluationService.faithfulness(answer, chunks);
        double relevancy = evaluationService.answerRelevancy(question, answer);

        queryLogService.save(question, answer, latencyMs, avgChunkScore, faithfulness, relevancy);

        RagResponse response = new RagResponse(answer, chunks, faithfulness, relevancy, false);

        // Populate both cache layers
        semanticCacheService.put(question, response);

        return response;
    }

    public String queryFallback(String question, Throwable t) {
        log.error("LLM circuit open or retries exhausted for query: {}", t.getMessage());
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }

    public RagResponse queryWithEvalFallback(String question, Throwable t) {
        log.error("LLM circuit open or retries exhausted for queryWithEval: {}", t.getMessage());
        return new RagResponse(
                "The AI service is temporarily unavailable. Please try again shortly.",
                List.of(), 0.0, 0.0
        );
    }
}
