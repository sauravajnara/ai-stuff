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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

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

    public RagQueryService(EmbeddingStore<TextSegment> embeddingStore,
                           EmbeddingModel embeddingModel,
                           ChatLanguageModel chatModel,
                           RagEvaluationService evaluationService,
                           QueryLogService queryLogService) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.evaluationService = evaluationService;
        this.queryLogService = queryLogService;
    }

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

    public RagResponse queryWithEval(String question) {
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

        Prompt prompt = PROMPT_TEMPLATE.apply(Map.of(
                "context", context,
                "question", question
        ));

        String answer = chatModel.chat(prompt.toUserMessage()).aiMessage().text();
        long latencyMs = System.currentTimeMillis() - start;

        double faithfulness = evaluationService.faithfulness(answer, chunks);
        double relevancy = evaluationService.answerRelevancy(question, answer);

        queryLogService.save(question, answer, latencyMs, avgChunkScore, faithfulness, relevancy);

        return new RagResponse(answer, chunks, faithfulness, relevancy);
    }
}
